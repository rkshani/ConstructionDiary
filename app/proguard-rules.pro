# Construction Diary release (R8) rules.
# Room, Hilt, WorkManager, Compose and DataStore ship their own consumer rules.

# Better crash stack traces without exposing source file names.
-keepattributes LineNumberTable
-renamesourcefileattribute SourceFile
