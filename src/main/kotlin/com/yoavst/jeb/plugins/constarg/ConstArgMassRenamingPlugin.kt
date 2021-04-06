package com.yoavst.jeb.plugins.constarg

import com.pnfsoftware.jeb.core.*
import com.pnfsoftware.jeb.core.units.code.android.IDexUnit
import com.yoavst.jeb.bridge.UIBridge
import com.yoavst.jeb.plugins.JEB_VERSION
import com.yoavst.jeb.plugins.PLUGIN_VERSION
import com.yoavst.jeb.utils.BasicEnginesPlugin
import com.yoavst.jeb.utils.decompiler
import com.yoavst.jeb.utils.displayFileOpenSelector
import com.yoavst.jeb.utils.renaming.RenameEngine
import java.io.File

class ConstArgMassRenamingPlugin :
    BasicEnginesPlugin(
        supportsClassFilter = true,
        defaultForScopeOnThisClass = false,
        defaultForScopeOnThisFunction = false,
    ) {
    private lateinit var signatures: Map<String, ExtendedRenamer>
    override fun getPluginInformation(): IPluginInformation = PluginInformation(
        "Const arg mass renaming plugin",
        "Fire the plugin to change names using information from a constant string argument to function",
        "Yoav Sternberg",
        PLUGIN_VERSION,
        JEB_VERSION,
        null
    )

    override fun getExecutionOptionDefinitions(): List<IOptionDefinition> {
        return super.getExecutionOptionDefinitions() + BooleanOptionDefinition(
            USE_BUILTIN,
            true,
            """Use the builtin method signature list. It supports Bundle, Intent, ContentValues and shared preferences.
If you have a suggestion to add to the global list, Please contact Yoav Sternberg."""
        )
    }

    override fun processOptions(executionOptions: Map<String, String>): Boolean {
        super.processOptions(executionOptions)

        if (!executionOptions.getOrDefault(USE_BUILTIN, "true").toBoolean()) {
            val scriptPath = File(displayFileOpenSelector("Signatures file") ?: run {
                logger.error("No script selected")
                return false
            })

            try {
                signatures = RenameSignaturesFileParser.parseSignatures(scriptPath.readText(), scriptPath.absoluteFile.parent)
            } catch (e: RuntimeException) {
                logger.catching(e, "Failed to parse signature file")
                return false
            }
        } else {
            signatures =
                RenameSignaturesFileParser.parseSignatures(javaClass.classLoader.getResource("rename_signatures.md")!!.readText(), ".")
        }
        return true
    }

    override fun processUnit(unit: IDexUnit, renameEngine: RenameEngine) {
        val massRenamer = ConstArgMassRenaming(
            signatures, isOperatingOnlyOnThisClass, classFilter
        )

        if (isOperatingOnlyOnThisMethod) {
            if (UIBridge.currentMethod != null && UIBridge.currentClass != null) {
                // you cannot see the sources of a type without implementing class
                massRenamer.processMethod(UIBridge.currentMethod!!, unit, unit.decompiler, renameEngine)
            }
        } else {
            massRenamer.processUnit(unit, renameEngine)
        }

        massRenamer.propagate(unit, renameEngine)
    }

    companion object {
        private const val USE_BUILTIN = "use builtin"
    }
}