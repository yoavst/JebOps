package com.yoavst.jeb.plugins.constarg

import com.pnfsoftware.jeb.core.IPluginInformation
import com.pnfsoftware.jeb.core.PluginInformation
import com.pnfsoftware.jeb.core.units.code.android.IDexUnit
import com.pnfsoftware.jeb.core.units.code.android.dex.IDexMethod
import com.yoavst.jeb.plugins.JEB_VERSION
import com.yoavst.jeb.plugins.PLUGIN_VERSION
import com.yoavst.jeb.utils.*
import com.yoavst.jeb.utils.renaming.RenameEngine

class GetXPlugin : BasicEnginesPlugin(supportsClassFilter = true, defaultForScopeOnThisClass = false) {
    override fun getPluginInformation(): IPluginInformation = PluginInformation(
            "GetX plugin",
            "Fire the plugin to scan the apk for getX methods and use that for naming",
            "Yoav Sternberg",
            PLUGIN_VERSION,
            JEB_VERSION,
            null
    )

    override fun processUnit(unit: IDexUnit, renameEngine: RenameEngine) {
        val visitor = simpleNameMethodVisitor(renameEngine)
        val renamers = unit.methods.asSequence().mapToPairNotNull(visitor).associate { (method, result) ->
            method.currentSignature to result
        }
        ConstArgMassRenaming(renamers, isOperatingOnlyOnThisClass, classFilter, recursive = false).processUnit(unit, renameEngine)
    }


    private fun simpleNameMethodVisitor(renameEngine: RenameEngine): (IDexMethod) -> ExtendedRenamer? = { method ->
        val currentName = method.currentName
        val name = renameEngine.getModifiedInfo(currentName)?.let { (realName, _) -> realName } ?: currentName
        if (name.startsWith("get") && name.length >= 4) {
            val fieldName = name.substring(3).decapitalize()
            when {
                method.parameterTypes.size == 0 -> {
                    ExtendedRenamer(-1, { RenameResult(assigneeName = fieldName) }, -1)
                }
                method.isStatic && method.parameterTypes.size == 1 -> {
                    ExtendedRenamer(-1, { RenameResult(argumentName = fieldName) }, 0)
                }
                else -> null
            }
        } else if (name.startsWith("set") && name.length >= 4) {
            val fieldName = name.substring(3).decapitalize()
            when {
                method.parameterTypes.size == 1 -> {
                    ExtendedRenamer(-1, { RenameResult(argumentName = fieldName) }, 0)
                }
                method.isStatic && method.parameterTypes.size == 2 -> {
                    ExtendedRenamer(-1, { RenameResult(argumentName = fieldName) }, 1)
                }
                else -> null
            }
        } else null
    }

}
