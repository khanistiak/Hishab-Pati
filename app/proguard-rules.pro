# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Keep Jetpack Credential Manager Play Services provider classes loaded via reflection
-keep class androidx.credentials.** { *; }
-keep class androidx.credentials.playservices.** { *; }
-dontwarn androidx.credentials.**

# Keep Google Identity Services (googleid) credential classes & builders
-keep class com.google.android.libraries.identity.googleid.** { *; }
-dontwarn com.google.android.libraries.identity.googleid.**

# Keep Firebase Authentication & Google Play Services Auth classes used during token exchange
-keep class com.google.firebase.auth.** { *; }
-keep class com.google.android.gms.auth.api.identity.** { *; }
-dontwarn com.google.android.gms.auth.api.identity.**
-keep class com.google.android.gms.auth.api.signin.** { *; }
-dontwarn com.google.android.gms.auth.api.signin.**
-keep class com.google.android.gms.common.** { *; }
-keep class com.google.android.gms.tasks.** { *; }
-keepattributes Signature,InnerClasses,EnclosingMethod, RuntimeVisibleAnnotations

