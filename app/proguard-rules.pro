-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class fr.leonelal.mtgduel.** {
    *** Companion;
}
-keepclasseswithmembers class fr.leonelal.mtgduel.** {
    kotlinx.serialization.KSerializer serializer(...);
}
