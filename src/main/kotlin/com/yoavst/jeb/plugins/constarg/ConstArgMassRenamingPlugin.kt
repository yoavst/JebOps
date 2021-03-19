package com.yoavst.jeb.plugins.constarg

import com.pnfsoftware.jeb.core.IPluginInformation
import com.pnfsoftware.jeb.core.PluginInformation
import com.pnfsoftware.jeb.core.Version
import com.pnfsoftware.jeb.core.units.code.android.IDexUnit
import com.yoavst.jeb.bridge.UIBridge
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
        Version.create(0, 1, 0),
        Version.create(3, 0, 16),
        null
    )

    override fun processOptions(executionOptions: Map<String, String>): Boolean {
        super.processOptions(executionOptions)

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
}