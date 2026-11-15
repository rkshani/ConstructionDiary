package com.diary.construction.data.backup

import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import com.diary.construction.data.local.AppDatabase
import com.diary.construction.data.photo.PhotoStorage
import com.diary.construction.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.util.Properties
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.system.exitProcess

/**
 * Offline backup: one .zip with the database and all photos, saved where the user picks
 * (Storage Access Framework). Restore replaces everything and restarts the app.
 */
@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: AppDatabase,
    private val photos: PhotoStorage,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    sealed interface Result {
        data object Ok : Result
        data object NotABackup : Result
        data class Failed(val message: String) : Result
    }

    private val dbFile: File get() = context.getDatabasePath(AppDatabase.NAME)

    suspend fun backup(uri: Uri): Result = withContext(io) {
        runCatching {
            // Move everything from the WAL file into the main database file first.
            db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(TRUNCATE)").use { it.moveToFirst() }
            context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
                ZipOutputStream(out).use { zip ->
                    val info = Properties().apply {
                        setProperty("app", context.packageName)
                        setProperty("dbVersion", db.openHelper.readableDatabase.version.toString())
                        setProperty("createdAt", System.currentTimeMillis().toString())
                        setProperty("photosDir", photos.photosDir.absolutePath)
                    }
                    zip.putNextEntry(ZipEntry(INFO))
                    info.store(zip, "Construction Diary backup")
                    zip.closeEntry()

                    zip.putNextEntry(ZipEntry(DB_ENTRY))
                    FileInputStream(dbFile).use { it.copyTo(zip) }
                    zip.closeEntry()

                    photos.photosDir.listFiles()?.filter { it.isFile }?.forEach { f ->
                        zip.putNextEntry(ZipEntry(PHOTOS_PREFIX + f.name))
                        FileInputStream(f).use { it.copyTo(zip) }
                        zip.closeEntry()
                    }
                }
            } ?: error("Cannot open the file")
            Result.Ok
        }.getOrElse { Result.Failed(it.message ?: it.javaClass.simpleName) }
    }

    /**
     * Restores a backup. On success the app process is restarted, so this only returns on
     * failure.
     */
    suspend fun restore(uri: Uri): Result = withContext(io) {
        val temp = File(context.cacheDir, "restore").apply { deleteRecursively(); mkdirs() }
        try {
            // 1. Unpack into a temp folder (with zip-slip protection).
            var info = Properties()
            context.contentResolver.openInputStream(uri)?.use { input ->
                ZipInputStream(input).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory) {
                            val target = File(temp, entry.name)
                            if (!target.canonicalPath.startsWith(temp.canonicalPath + File.separator)) error("Bad entry")
                            target.parentFile?.mkdirs()
                            target.outputStream().use { zip.copyTo(it) }
                            if (entry.name == INFO) info = Properties().apply { target.inputStream().use { load(it) } }
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
            } ?: return@withContext Result.Failed("Cannot open the file")

            val newDb = File(temp, DB_ENTRY)
            if (!newDb.exists() || !isSqlite(newDb)) return@withContext Result.NotABackup

            // 2. Fix saved photo paths if the app folder changed (other phone / user).
            val oldDir = info.getProperty("photosDir").orEmpty()
            val newDir = photos.photosDir.absolutePath
            if (oldDir.isNotEmpty() && oldDir != newDir) rewritePhotoPaths(newDb, oldDir, newDir)

            // 3. Swap the database and photos.
            db.close()
            listOf("", "-wal", "-shm", "-journal").forEach { File(dbFile.path + it).delete() }
            dbFile.parentFile?.mkdirs()
            newDb.copyTo(dbFile, overwrite = true)

            val photoDir = photos.photosDir
            photoDir.listFiles()?.forEach { it.delete() }
            File(temp, PHOTOS_PREFIX.trimEnd('/')).listFiles()?.forEach { it.copyTo(File(photoDir, it.name), overwrite = true) }
            temp.deleteRecursively()

            restartApp()
            Result.Ok
        } catch (e: Exception) {
            temp.deleteRecursively()
            Result.Failed(e.message ?: e.javaClass.simpleName)
        }
    }

    private fun isSqlite(file: File): Boolean = runCatching {
        val header = ByteArray(16)
        FileInputStream(file).use { it.read(header) }
        String(header, Charsets.US_ASCII).startsWith("SQLite format 3")
    }.getOrDefault(false)

    private fun rewritePhotoPaths(file: File, oldDir: String, newDir: String) {
        SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READWRITE).use { sql ->
            sql.execSQL("UPDATE photos SET path = REPLACE(path, ?, ?)", arrayOf(oldDir, newDir))
            sql.execSQL("UPDATE material_entries SET billPhotoPath = REPLACE(billPhotoPath, ?, ?) WHERE billPhotoPath IS NOT NULL", arrayOf(oldDir, newDir))
            sql.execSQL("UPDATE expenses SET billPhotoPath = REPLACE(billPhotoPath, ?, ?) WHERE billPhotoPath IS NOT NULL", arrayOf(oldDir, newDir))
        }
    }

    private fun restartApp() {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        if (launch != null) context.startActivity(launch)
        exitProcess(0)
    }

    private companion object {
        const val INFO = "backup.properties"
        const val DB_ENTRY = "database/" + AppDatabase.NAME
        const val PHOTOS_PREFIX = PhotoStorage.PHOTOS_DIR + "/"
    }
}
