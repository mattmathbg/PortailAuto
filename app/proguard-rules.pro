# Proguard rules for Portail CROUS Gate Opener
# Keep DataStore & Play Services
-keepclassmembers class * extends androidx.datastore.preferences.core.Preferences { *; }
-keep class com.google.android.gms.location.** { *; }
-dontwarn com.google.android.gms.location.**
