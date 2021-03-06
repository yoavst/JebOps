package com.yoavst.jeb.plugins.constarg

import com.pnfsoftware.jeb.core.*
import com.pnfsoftware.jeb.core.units.code.android.IDexUnit
import com.pnfsoftware.jeb.core.units.code.android.dex.IDexMethod
import com.yoavst.jeb.bridge.UIBridge
import com.yoavst.jeb.utils.BasicEnginesPlugin
import com.yoavst.jeb.utils.displayFileOpenSelector
import com.yoavst.jeb.utils.originalSignature
import com.yoavst.jeb.utils.renaming.RenameEngine
import kotlin.properties.Delegates

class ConstArgRenamingPlugin :
    BasicEnginesPlugin(
        supportsClassFilter = true,
        defaultForScopeOnThisClass = false,
        defaultForScopeOnThisFunction = false,
        usingSelectedMethod = true
    ) {

    private var constArgumentIndex by Delegates.notNull<Int>()
    private lateinit var renameMethod: IDexMethod

    override fun getPluginInformation(): IPluginInformation = PluginInformation(
        "Const arg renaming plugin",
        "Fire the plugin to change names using information from a constant string argument to function",
        "Yoav Sternberg",
        Version.create(0, 1, 0),
        Version.create(3, 0, 16),
        null
    )

    override fun getExecutionOptionDefinitions(): List<IOptionDefinition> {
        val options = super.getExecutionOptionDefinitions().toMutableList()
        options += ListOptionDefinition(
            TargetRenameTag,
            RenameTarget.Class.name,
            "What do we want to renamed based on the argument",
            *RenameTarget.values().map(RenameTarget::name).toTypedArray()
        )
        options += OptionDefinition(
            TargetConstArgIndex,
            "0",
            "What is the index of the const argument that will be used as name?"
        )
        options += OptionDefinition(
            TargetArgumentToBeRenamedPosition,
            "Optional: If renaming an argument, what is its index"
        )
        return options
    }

    override fun processOptions(executionOptions: Map<String, String>): Boolean {
        super.processOptions(executionOptions)
        val renamedMethodTemp = UIBridge.focusedMethod
        if (renamedMethodTemp == null) {
            logger.error("A method must be focus for this plugin to work")
            return false
        }
        renameMethod = renamedMethodTemp

        constArgumentIndex = executionOptions[TargetConstArgIndex]?.toIntOrNull() ?: run {
            logger.error("Index of the const argument must be a number")
            return false
        }
        if (constArgumentIndex < 0 || renameMethod.parameterTypes.size <= constArgumentIndex) {
            logger.error("The const argument index is out of range: [0, ${renameMethod.parameterTypes.size})")
            return false
        }
        val paramType = renameMethod.parameterTypes[constArgumentIndex].originalSignature
        if (paramType != "Ljava/lang/CharSequence;" && paramType != "Ljava/lang/String;") {
            logger.error("The argument type is not a string type. Received: $paramType")
            return false
        }

        val targetRenameStr = executionOptions[TargetRenameTag] ?: ""
        if (targetRenameStr.isBlank()) {
            logger.error("Target for renaming is not provided")
            return false
        }
        // safe because it comes from list
        val targetRename = RenameTarget.valueOf(targetRenameStr)
        if (targetRename == RenameTarget.Custom) {
            val script = displayFileOpenSelector("Renaming script") ?: run {
                    logger.error("No script selected")
                    return false
                }
        }

        return true
    }

    override fun processUnit(unit: IDexUnit, renameEngine: RenameEngine) {
        TODO("Not yet implemented")

    }

    companion object {
        private const val TargetRenameTag = "Target rename"
        private const val TargetConstArgIndex = "Const arg index"
        private const val TargetArgumentToBeRenamedPosition = "renamed arg index"
    }
}