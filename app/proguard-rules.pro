# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep model classes
-keep class com.formulacalc.model.** { *; }

# Keep parser classes
-keep class com.formulacalc.parser.** { *; }
