# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**
-keepclassmembers class * {
    @androidx.room.* <fields>;
    @androidx.room.* <methods>;
}

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keepclassmembers class com.eggrice.timetable.** { <fields>; }
-keep class com.eggrice.timetable.data.entity.** { *; }
-keep class com.eggrice.timetable.network.** { *; }
-keep class com.eggrice.timetable.ui.import_.ImportCourseJson { *; }

# OkHttp + Okio
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# Coroutines
-keepclassmembernames class kotlinx.coroutines.** {
    volatile <fields>;
}

# Compose
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }

# Keep data classes used in network/DB
-keepclassmembers class com.eggrice.timetable.** {
    !transient <fields>;
}
