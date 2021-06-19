package com.yoavst.jeb.plugins.kotlin

import com.pnfsoftware.jeb.core.IPluginInformation
import com.pnfsoftware.jeb.core.PluginInformation
import com.pnfsoftware.jeb.core.units.code.android.IDexUnit
import com.pnfsoftware.jeb.core.units.code.android.dex.IDexClass
import com.yoavst.jeb.plugins.JEB_VERSION
import com.yoavst.jeb.plugins.PLUGIN_VERSION
import com.yoavst.jeb.plugins.constarg.ConstArgMassRenaming
import com.yoavst.jeb.utils.BasicEnginesPlugin
import com.yoavst.jeb.utils.renaming.RenameEngine
import com.yoavst.jeb.utils.renaming.RenameReason
import com.yoavst.jeb.utils.renaming.RenameRequest
import com.yoavst.jeb.utils.xrefsForString

class KotlinIntrinsicsPlugin : BasicEnginesPlugin(supportsClassFilter = true) {
    override fun getPluginInformation(): IPluginInformation = PluginInformation(
            "Kotlin Intrinsics processor",
            "Fire the plugin to process kotlin intrinsics",
            "Yoav Sternberg",
            PLUGIN_VERSION,
            JEB_VERSION,
            null
    )

    override fun processUnit(unit: IDexUnit, renameEngine: RenameEngine) {
        val foundStrings = unit.strings?.filter { it.toString() in EXPECTED_STRINGS } ?: emptyList()
        if (foundStrings.isEmpty()) {
            logger.error("No Strings from Kotlin.Intrinsics was found in the unit: $unit")
            return
        }
        val results = foundStrings.fold(mutableMapOf<IDexClass, Int>()) { cache, str ->
            unit.xrefsForString(str).forEach { xref ->
                val cls = unit.getMethod(xref)?.classType?.implementingClass ?: return@forEach
                cache[cls] = (cache[cls] ?: 0) + 1
            }
            cache
        }
        val result = results.maxByOrNull { it.value }
        if (result == null) {
            logger.error("Could not found Kotlin.Intrinsics!")
            return
        }

        val intrinsicsClass = result.key

        renameEngine.renameClass(RenameRequest("Intrinsics", RenameReason.KotlinName), intrinsicsClass)

        val renamers = intrinsicsClass.methods.mapNotNull { method ->
            val methodResult = IntrinsicsMethodDetector.detect(method, unit) ?: return@mapNotNull null
            renameEngine.renameMethod(RenameRequest(methodResult.name, RenameReason.KotlinName), method, method.classType.implementingClass)
            if (methodResult.renamer != null) {
                method.getSignature(false) to methodResult.renamer
            } else null
        }

        val massRenamer = ConstArgMassRenaming(renamers.toMap(), false, classFilter)

        massRenamer.processUnit(unit, renameEngine)
        massRenamer.propagate(unit, renameEngine)
    }

    companion object {
        val EXPECTED_STRINGS = setOf(
                "lateinit property ",
                " has not been initialized",
                " must not be null",
                "Method specified as non-null returned null: ",
                "Field specified as non-null is null: ",
                "Parameter specified as non-null is null: method ",
                "This function has a reified type parameter and thus can only be inlined at compilation time, not called directly.",
                " is not found. Please update the Kotlin runtime to the latest version",
                " is not found: this code requires the Kotlin runtime of version at least "
        )
    }
}