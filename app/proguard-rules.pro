# Construction Diary release (R8) rules.
# Room, Hilt, WorkManager, Compose, DataStore and Biometric ship their own consumer rules.

# Enum names are saved in the database and in settings (ProjectStatus, ThemeMode, ...).
# Keep their names so saved data still matches after shrinking.
-keepclassmembers enum com.diary.construction.domain.model.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    <fields>;
}

# WorkManager creates the reminder worker by reflection (through the Hilt worker factory).
-keep class com.diary.construction.data.reminder.ReminderWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# Better crash stack traces without exposing source file names.
-keepattributes LineNumberTable
-renamesourcefileattribute SourceFile
