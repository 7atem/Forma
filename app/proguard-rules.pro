# Room, Hilt, ML Kit and kotlinx.serialization publish their consumer rules.
# Native SQLCipher methods and Android entry points are retained by the default
# Android optimized rules plus dependency consumer rules. No reflection-only
# application model serializer is used.
-keepattributes Signature,InnerClasses,EnclosingMethod
