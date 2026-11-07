# NetTrace Production Proguard / R8 Optimization Rules

# Keep application entry points
-keep class com.example.NetTraceApplication { *; }
-keep class com.example.MainActivity { *; }

# WorkManager reflection instantiation
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class * extends androidx.work.Worker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class com.example.worker.SimUsageSamplingWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class androidx.work.impl.WorkManagerImpl { *; }
-dontwarn androidx.work.**

# DataStore & Protobuf serialization preservation
-keepclassmembers class * extends androidx.datastore.preferences.core.Preferences {
    *;
}
-keep class androidx.datastore.** { *; }
-dontwarn androidx.datastore.**

# Compose runtime optimizations
-keepclassmembers class * extends androidx.compose.runtime.State { *; }
-dontwarn androidx.compose.**

# Retain inner classes and annotations
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
