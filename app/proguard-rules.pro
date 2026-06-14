# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\VD\AppData\Local\Android\sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Check your dependencies proguard rules as they might override this.
# By default, R8 will obfuscate all your app code (including Security.kt and BillingManager.kt) 
# because there are no -keep rules for them.

# Flatten package hierarchy to make reversing harder
-repackageclasses ''

# Strip out logs to prevent exposing verification logic via logcat during runtime analysis
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

# Obfuscate strings (Note: R8 does basic string sharing; dedicated tools like DexGuard are better for string encryption)
