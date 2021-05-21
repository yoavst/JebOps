package com.yoavst.jeb.plugins.kotlin

import com.pnfsoftware.jeb.core.IOptionDefinition
import com.pnfsoftware.jeb.core.IPluginInformation
import com.pnfsoftware.jeb.core.OptionDefinition
import com.pnfsoftware.jeb.core.PluginInformation
import com.pnfsoftware.jeb.core.units.code.android.IDexUnit
import com.pnfsoftware.jeb.core.units.code.android.dex.IDexAnnotation
import com.pnfsoftware.jeb.core.units.code.android.dex.IDexClass
import com.pnfsoftware.jeb.core.units.code.android.dex.IDexValue
import com.yoavst.jeb.bridge.UIBridge
import com.yoavst.jeb.plugins.JEB_VERSION
import com.yoavst.jeb.plugins.PLUGIN_VERSION
import com.yoavst.jeb.utils.*
import com.yoavst.jeb.utils.renaming.RenameEngine
import com.yoavst.jeb.utils.renaming.RenameReason
import com.yoavst.jeb.utils.renaming.RenameRequest
import kotlinx.metadata.jvm.KotlinClassHeader
import kotlinx.metadata.jvm.KotlinClassMetadata

class KotlinPlugin : BasicEnginesPlugin(supportsClassFilter = true, defaultForScopeOnThisClass = false) {
    private lateinit var annotationSignature: String
    private lateinit var metadataKName: String
    private lateinit var metadataBVName: String
    private lateinit var metadataMVName: String
    private lateinit var metadataD1Name: String
    private lateinit var metadataD2Name: String
    private lateinit var metadataXSName: String
    private lateinit var metadataPNName: String
    private lateinit var metadataXIName: String

    private var metadataKIndex: Int = -1
    private var metadataBVIndex: Int = -1
    private var metadataMVIndex: Int = -1
    private var metadataD1Index: Int = -1
    private var metadataD2Index: Int = -1
    private var metadataXSIndex: Int = -1
    private var metadataPNIndex: Int = -1
    private var metadataXIIndex: Int = -1

    override fun getPluginInformation(): IPluginInformation = PluginInformation(
            "Kotlin metadata processor",
            "Fire the plugin to process kotlin metadata to get class names",
            "Yoav Sternberg",
            PLUGIN_VERSION,
            JEB_VERSION,
            null
    )

    override fun getExecutionOptionDefinitions(): List<IOptionDefinition> {
        return super.getExecutionOptionDefinitions() + OptionDefinition(
                KOTLIN_METADATA_SIGNATURE,
                "Lkotlin/Metadata;",
                """The signature of the kotlin metadata annotation. Default is to use the unobfuscated name."""
        ) + OptionDefinition(
                KOTLIN_METADATA_KIND,
                KOTLIN_METADATA_KIND,
                """The name of the kind parameter. Original name is: k. An integer - value is between 1 to 5. 1 for normal classes. """
        ) + OptionDefinition(
                KOTLIN_METADATA_BYTECODE_VERSION,
                KOTLIN_METADATA_BYTECODE_VERSION,
                """The name of the bytecode version parameter. Original name is: bv. An int array - usually {1,0,3}."""
        ) + OptionDefinition(
                KOTLIN_METADATA_METADATA_VERSION,
                KOTLIN_METADATA_METADATA_VERSION,
                """The name of the metadata version parameter. Original name is: mv. An int array - The one that is not the bytecode version."""
        ) + OptionDefinition(
                KOTLIN_METADATA_DATA1,
                KOTLIN_METADATA_DATA1,
                """The name of the data1 parameter. Original name is: d1. A string array - Usually contains one long protobuf binary string."""
        ) + OptionDefinition(
                KOTLIN_METADATA_DATA2,
                KOTLIN_METADATA_DATA2,
                """The name of the data2 parameter. Original name is: d2. A string array - Usually consists of many strings."""
        ) + OptionDefinition(
                KOTLIN_METADATA_PACKAGE_NAME,
                KOTLIN_METADATA_PACKAGE_NAME,
                """The name of the package name parameter (since Kotlin 1.2). Original name is: pn. """
        ) + OptionDefinition(
                KOTLIN_METADATA_EXTRA_STRING,
                KOTLIN_METADATA_EXTRA_STRING,
                """The name of the extra string parameter. Original name is: xs. A String - Usually doesn't have a value, it's the name of the facade class."""
        ) + OptionDefinition(
                KOTLIN_METADATA_EXTRA_INT,
                KOTLIN_METADATA_EXTRA_INT,
                """The name of the extra int parameter (since Kotlin 1.1). Original name is: xi. An int - Value is between 0 to 6, usually not available"""
        )
    }

    override fun processOptions(executionOptions: Map<String, String>): Boolean {
        super.processOptions(executionOptions)
        annotationSignature = executionOptions.getOrElse(KOTLIN_METADATA_SIGNATURE) { "Lkotlin/Metadata;" }

        metadataKName = executionOptions.getOrElse(KOTLIN_METADATA_KIND) { KOTLIN_METADATA_KIND }
        metadataBVName = executionOptions.getOrElse(KOTLIN_METADATA_BYTECODE_VERSION) { KOTLIN_METADATA_BYTECODE_VERSION }
        metadataMVName = executionOptions.getOrElse(KOTLIN_METADATA_METADATA_VERSION) { KOTLIN_METADATA_METADATA_VERSION }
        metadataD1Name = executionOptions.getOrElse(KOTLIN_METADATA_DATA1) { KOTLIN_METADATA_DATA1 }
        metadataD2Name = executionOptions.getOrElse(KOTLIN_METADATA_DATA2) { KOTLIN_METADATA_DATA2 }
        metadataXSName = executionOptions.getOrElse(KOTLIN_METADATA_EXTRA_STRING) { KOTLIN_METADATA_EXTRA_STRING }
        metadataPNName = executionOptions.getOrElse(KOTLIN_METADATA_PACKAGE_NAME) { KOTLIN_METADATA_PACKAGE_NAME }
        metadataXIName = executionOptions.getOrElse(KOTLIN_METADATA_EXTRA_INT) { KOTLIN_METADATA_EXTRA_INT }

        return true
    }

    override fun processUnit(unit: IDexUnit, renameEngine: RenameEngine) {
        val annotationClass = unit.classBySignature(annotationSignature)
        if (annotationClass == null) {
            logger.error("No such class '$annotationSignature' in this dex unit")
            return
        } else if (annotationClass.accessFlags and IDexUnit.ACC_ANNOTATION == 0) {
            logger.error("Class '$annotationClass' is not an annotation class.")
            return
        }

        metadataKIndex = annotationClass.methods.firstOrNull { it.originalName == metadataKName }?.nameIndex ?: run {
            logger.error("The kind parameter does not exist for the annotation class")
            return
        }
        metadataBVIndex = annotationClass.methods.firstOrNull { it.originalName == metadataBVName }?.nameIndex ?: run {
            logger.error("The bytecode version parameter does not exist for the annotation class")
            return
        }
        metadataMVIndex = annotationClass.methods.firstOrNull { it.originalName == metadataMVName }?.nameIndex ?: run {
            logger.error("The metadata version parameter does not exist for the annotation class")
            return
        }
        metadataD2Index = annotationClass.methods.firstOrNull { it.originalName == metadataD2Name }?.nameIndex ?: run {
            logger.error("The data2 parameter does not exist for the annotation class")
            return
        }
        metadataXSIndex = annotationClass.methods.firstOrNull { it.originalName == metadataXSName }?.nameIndex ?: run {
            logger.error("The extra string parameter does not exist for the annotation class")
            return
        }
        metadataD1Index = annotationClass.methods.firstOrNull { it.originalName == metadataD1Name }?.nameIndex ?: run {
            logger.error("The data1 parameter does not exist for the annotation class")
            return
        }
        metadataPNIndex = annotationClass.methods.firstOrNull { it.originalName == metadataPNName }?.nameIndex ?: run {
            logger.error("The package name parameter does not exist for the annotation class. Ignoring the parameter")
            -1
        }
        metadataXIIndex = annotationClass.methods.firstOrNull { it.originalName == metadataXIName }?.nameIndex ?: run {
            logger.error("The extra int parameter does not exist for the annotation class. Ignoring the parameter")
            -1
        }

        val annotationTypeIndex = annotationClass.classTypeIndex

        var seq = unit.classes.asSequence()
        seq = if (isOperatingOnlyOnThisClass) {
            seq.filter { it.classType == UIBridge.currentClass }
        } else {
            seq.filter(classFilter::matches)
        }

        var i = 0
        seq.filter { it.annotationsDirectory?.classAnnotations?.isNotEmpty() ?: false }.mapToPairNotNull { cls ->
            cls.annotationsDirectory.classAnnotations.firstOrNull {
                it.annotation.typeIndex == annotationTypeIndex
            }?.annotation
        }.forEach { (cls, annotation) ->
            i++
            processClass(unit, cls, annotation, renameEngine)
        }
        logger.info("There are $i classes with kotlin metadata annotation!")
    }

    private fun processClass(unit: IDexUnit, cls: IDexClass, annotation: IDexAnnotation, renameEngine: RenameEngine) {
        val elements = annotation.elements.sortedBy { it.nameIndex }
        val k = elements.firstOrNull { it.nameIndex == metadataKIndex }?.value?.int
        val mv = elements.firstOrNull { it.nameIndex == metadataMVIndex }?.value?.parseIntArray()
        val d1 = elements.firstOrNull { it.nameIndex == metadataD1Index }?.value?.parseStringArray(unit)?.map { it.unescape() }?.toTypedArray()
        val d2 = elements.firstOrNull { it.nameIndex == metadataD2Index }?.value?.parseStringArray(unit)
        val pn = elements.firstOrNull { it.nameIndex == metadataPNIndex }?.value?.stringIndex?.let(unit::getString)?.toString()
        val xs = elements.firstOrNull { it.nameIndex == metadataXSIndex }?.value?.stringIndex?.let(unit::getString)?.toString()
        val xi = elements.firstOrNull { it.nameIndex == metadataXIIndex }?.value?.int


        val header = KotlinClassHeader(k, mv, d1, d2, xs, pn, xi)
        val originalComment = unit.getComment(cls.currentSignature) ?: ""
        if (KOTLIN_METADATA_COMMENT_PREFIX !in originalComment) {
            val comment = header.toStringBlock()
            if (originalComment.isBlank()) {
                unit.setComment(cls.currentSignature,  comment)
             } else {
                unit.setComment(cls.currentSignature, originalComment + "\n\n" + comment)
            }
        }

        when (val metadata = KotlinClassMetadata.read(header)) {
            is KotlinClassMetadata.Class -> {
                val classInfo = metadata.toKmClass()
                val name = classInfo.name.split("/").last().replace(".", "$")
                renameEngine.renameClass(RenameRequest(name, RenameReason.KotlinName), cls)
            }
            is KotlinClassMetadata.FileFacade -> Unit
            is KotlinClassMetadata.SyntheticClass -> Unit
            is KotlinClassMetadata.MultiFileClassFacade -> Unit
            is KotlinClassMetadata.MultiFileClassPart -> Unit
            is KotlinClassMetadata.Unknown -> {
                logger.warning("Encountered unknown class: ${cls.currentName}. It probably means the metadata lib version is old.")
            }
            null -> Unit
        }
    }

    private fun IDexValue.parseStringArray(dexUnit: IDexUnit): Array<String> {
        assert(type == IDexValue.VALUE_ARRAY) { "The value of the given dex value must be array" }
        return array.map { dexUnit.getString(it.stringIndex).toString() }.toTypedArray()
    }

    private fun IDexValue.parseIntArray(): IntArray {
        assert(type == IDexValue.VALUE_ARRAY) { "The value of the given dex value must be array" }
        return array.map { it.int }.toIntArray()
    }

    companion object {
        private const val KOTLIN_METADATA_SIGNATURE = "Kotlin Metadata Signature"
        private const val KOTLIN_METADATA_KIND = "k"
        private const val KOTLIN_METADATA_METADATA_VERSION = "mv"
        private const val KOTLIN_METADATA_BYTECODE_VERSION = "bv"
        private const val KOTLIN_METADATA_DATA1 = "d1"
        private const val KOTLIN_METADATA_DATA2 = "d2"
        private const val KOTLIN_METADATA_EXTRA_STRING = "xs"
        private const val KOTLIN_METADATA_PACKAGE_NAME = "pn"
        private const val KOTLIN_METADATA_EXTRA_INT = "xi"
    }
}

