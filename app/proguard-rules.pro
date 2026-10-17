# TecOff ProGuard Rules
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.room.* *;
}
-keep class br.com.tecoff.data.model.** { *; }
-keep class br.com.tecoff.data.local.entity.** { *; }
