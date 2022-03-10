package com.yoavst.jeb.plugins.sourcefile

import com.pnfsoftware.jeb.core.BooleanOptionDefinition
import com.pnfsoftware.jeb.core.IOptionDefinition
import com.pnfsoftware.jeb.core.IPluginInformation
import com.pnfsoftware.jeb.core.PluginInformation
import com.pnfsoftware.jeb.core.units.code.android.IDexUnit
import com.pnfsoftware.jeb.core.units.code.android.dex.IDexClass
import com.yoavst.jeb.bridge.UIBridge
import com.yoavst.jeb.plugins.JEB_VERSION
import com.yoavst.jeb.plugins.PLUGIN_VERSION
import com.yoavst.jeb.utils.*
import com.yoavst.jeb.utils.renaming.RenameEngine
import com.yoavst.jeb.utils.renaming.RenameReason
import com.yoavst.jeb.utils.renaming.RenameRequest

class SourceFilePlugin : BasicEnginesPlugin(supportsClassFilter = true, defaultForScopeOnThisClass = false) {
    private var shouldAddComment: Boolean = false
    private var shouldAddToTypeName: Boolean = false
    override fun getPluginInformation(): IPluginInformation = PluginInformation(
            "Source file information",
            "Fire the plugin propagate source file info to decompilation view and to classes' name",
            "Yoav Sternberg",
            PLUGIN_VERSION,
            JEB_VERSION,
            null
    )

    override fun getExecutionOptionDefinitions(): List<IOptionDefinition> {
        return super.getExecutionOptionDefinitions() + BooleanOptionDefinition(
                ADD_COMMENT,
                true,
                """Add the original source file as comment to the class"""
        ) + BooleanOptionDefinition(
                ADD_TO_TYPE_NAME,
                false,
                """Add the original source file as part of the type"""
        )
    }

    override fun processOptions(executionOptions: Map<String, String>): Boolean {
        super.processOptions(executionOptions)
        shouldAddComment = executionOptions.getOrDefault(ADD_COMMENT, "true").toBoolean()
        shouldAddToTypeName = executionOptions.getOrDefault(ADD_TO_TYPE_NAME, "false").toBoolean()
        return true
    }

    override fun processUnit(unit: IDexUnit, renameEngine: RenameEngine) {
        if (!shouldAddComment && !shouldAddToTypeName)
            return

        var seq = unit.classes.asSequence()
        seq = if (isOperatingOnlyOnThisClass) {
            seq.filter { it.classType == UIBridge.currentClass }
        } else {
            seq.filter(classFilter::matches)
        }

        var i = 0
        seq.filter { it.sourceStringIndex != -1 }.forEach {
            i++
            processClass(unit, it, renameEngine)
        }
        logger.info("There are $i classes with source info")
    }

    private fun processClass(unit: IDexUnit, cls: IDexClass, renameEngine: RenameEngine) {
        val sourceName = unit.getString(cls.sourceStringIndex).value
        if (sourceName.isBlank()) return
        val sourceWithExtension = sourceName.split('.', limit = 2)[0]

        if (shouldAddComment) {
            if (sourceWithExtension !in cls.currentSignature && !cls.isMemberClass) {
                val originalComment = unit.getCommentBackport(cls.currentSignature) ?: ""
                if (COMMENT_PREFIX !in originalComment) {
                    val comment = "$COMMENT_PREFIX$sourceName"
                    if (originalComment.isBlank()) {
                        unit.setCommentBackport(cls.currentSignature, comment)
                    } else {
                        unit.setCommentBackport(cls.currentSignature, originalComment + "\n\n" + comment)
                    }
                }
            }
        }
        if (shouldAddToTypeName) {
            if (sourceWithExtension !in cls.currentSignature && !cls.isMemberClass) {
                renameEngine.renameClass(RenameRequest(sourceWithExtension, RenameReason.SourceFile), cls)
            }
        }
    }

    companion object {
        private const val ADD_COMMENT = "add comment"
        private const val ADD_TO_TYPE_NAME = "add to type name"
        private const val COMMENT_PREFIX = "source name: "
    }
}
