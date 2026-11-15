package com.diary.construction.di

import android.content.Context
import androidx.room.Room
import com.diary.construction.data.local.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME)
            .addCallback(AppDatabase.SEED_CALLBACK)
            .build()

    @Provides fun projectDao(db: AppDatabase) = db.projectDao()
    @Provides fun stageDao(db: AppDatabase) = db.stageDao()
    @Provides fun materialDao(db: AppDatabase) = db.materialDao()
    @Provides fun supplierDao(db: AppDatabase) = db.supplierDao()
    @Provides fun labourDao(db: AppDatabase) = db.labourDao()
    @Provides fun contractorDao(db: AppDatabase) = db.contractorDao()
    @Provides fun expenseDao(db: AppDatabase) = db.expenseDao()
    @Provides fun photoDao(db: AppDatabase) = db.photoDao()
}
