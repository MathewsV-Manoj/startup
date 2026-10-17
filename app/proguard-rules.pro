# Keep TFLite
-keep class org.tensorflow.lite.** { *; }
-dontwarn org.tensorflow.lite.**

# Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
