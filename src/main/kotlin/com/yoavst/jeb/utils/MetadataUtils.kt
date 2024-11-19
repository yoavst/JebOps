package com.yoavst.jeb.utils

import kotlinx.metadata.jvm.*

// taken from https://github.com/mforlini/KMparse/blob/master/src/main/kotlin/io/github/mforlini/kmparse/Extensions.kt
private const val INDENT = "\n|    "
const val KOTLIN_METADATA_COMMENT_PREFIX = "Kotlin metadata:"
fun KotlinClassHeader.toStringBlock(): String {
    return when (val metadata = KotlinClassMetadata.read(this)) {
        is KotlinClassMetadata.Class -> {
            val klass = metadata.toKmClass()
            """$KOTLIN_METADATA_COMMENT_PREFIX
                    |Type: Class
                    |Class Info:
                    |    Name: ${klass.name}
                    |    Supertypes: ${klass.supertypes.joinToString(", ") { it.classifier.toString() }}
                    |    Module Name: ${klass.moduleName}
                    |    Type Aliases: ${klass.typeAliases.joinToString(", ")}
                    |    Companion Object: ${klass.companionObject ?: ""}
                    |    Nested Classes:  ${klass.nestedClasses.joinToString(", ")}
                    |    Enum Entries: ${klass.enumEntries.joinToString(", ")}
                    |
                    |Constructors:${klass.constructors.joinToString(separator = INDENT, prefix = INDENT) { "${it.signature}, Arguments: ${it.valueParameters.joinToString(", ") { arg -> arg.name }}" }}
                    |
                    |Functions:${klass.functions.joinToString(separator = INDENT, prefix = INDENT) { "${it.signature}, Arguments: ${it.valueParameters.joinToString(", ") { arg -> arg.name }}" }}
                    |
                    |Properties:${klass.properties.joinToString(separator = INDENT, prefix = INDENT) { "${it.fieldSignature}" }}
        """.trimMargin()
        }

        is KotlinClassMetadata.FileFacade -> {
            val klass = metadata.toKmPackage()
            """$KOTLIN_METADATA_COMMENT_PREFIX
                    |Type: File Facade
                    |
                    |Functions:${klass.functions.joinToString(separator = INDENT, prefix = INDENT) { "${it.signature}, Arguments: ${it.valueParameters.joinToString(", ") { arg -> arg.name }}" }}
                    |
                    |Properties:${klass.properties.joinToString(separator = INDENT, prefix = INDENT) { "${it.fieldSignature}" }}
        """.trimMargin()
        }

        is KotlinClassMetadata.SyntheticClass -> {
            if (metadata.isLambda) {
                val klass = metadata.toKmLambda()
                """$KOTLIN_METADATA_COMMENT_PREFIX
                |Type: Synthetic Class
                |Is Kotlin Lambda: True
                |
                |Functions:
                |    ${klass?.function?.signature}, Arguments: ${klass?.function?.valueParameters?.joinToString(", ") { it.name }}
                
            """.trimMargin()

            } else {
                """$KOTLIN_METADATA_COMMENT_PREFIX
                |Type: Synthetic Class
                |Is Kotlin Lambda: False
            """.trimMargin()

            }
        }

        is KotlinClassMetadata.MultiFileClassFacade -> """$KOTLIN_METADATA_COMMENT_PREFIX
            |Type: Multi-File Class Facade
            |This multi-file class combines:
            |${metadata.partClassNames.joinToString(separator = INDENT, prefix = INDENT) { "Class: $it" }}
        """.trimMargin()

        is KotlinClassMetadata.MultiFileClassPart -> {
            val klass = metadata.toKmPackage()
            """$KOTLIN_METADATA_COMMENT_PREFIX
                    |Type: Multi-File Class Part
                    |Name: ${metadata.facadeClassName}
                    |
                    |Functions:${klass.functions.joinToString(separator = INDENT, prefix = INDENT) { "${it.signature}, Arguments: ${it.valueParameters.joinToString(", ") { arg -> arg.name }}" }}
                    |
                    |Properties:${klass.properties.joinToString(separator = INDENT, prefix = INDENT) { "${it.fieldSignature}" }}
        """.trimMargin()
        }

        is KotlinClassMetadata.Unknown -> """$KOTLIN_METADATA_COMMENT_PREFIX Type: Unknown"""
        null -> ""
    }
}
