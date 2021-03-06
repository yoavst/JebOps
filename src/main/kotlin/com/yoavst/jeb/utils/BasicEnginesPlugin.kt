package com.yoavst.jeb.utils

import com.pnfsoftware.jeb.core.AbstractEnginesPlugin
import com.pnfsoftware.jeb.core.IEnginesContext
import com.pnfsoftware.jeb.core.IOptionDefinition
import com.pnfsoftware.jeb.core.units.code.android.IDexUnit
import com.pnfsoftware.jeb.util.logging.GlobalLog
import com.pnfsoftware.jeb.util.logging.ILogger
import com.yoavst.jeb.bridge.UIBridge
import com.yoavst.jeb.utils.renaming.RenameEngine
import kotlin.properties.Delegates

abstract class BasicEnginesPlugin(
    private val supportsClassFilter: Boolean = false,
    private val defaultForScopeOnThisClass: Boolean? = null,
    private val defaultForScopeOnThisFunction: Boolean? = null
) : AbstractEnginesPlugin() {
    protected lateinit var context: IEnginesContext
    protected val logger: ILogger = GlobalLog.getLogger(javaClass)

    protected lateinit var classFilter: Regex
    protected var isOperatingOnlyOnThisClass: Boolean by Delegates.notNull()
    protected var isOperatingOnlyOnThisMethod: Boolean by Delegates.notNull()

    override fun execute(context: IEnginesContext, executionOptions: MutableMap<String, String>?) {
        this.context = context
        if (context.projects.isEmpty()) {
            logger.error("Error: Please open a project!")
            return
        }

        processOptions(executionOptions ?: mapOf())

        val renameEngine = RenameEngine.create()
        context.getDexUnits().forEach {
            processUnit(it, renameEngine)
            it.refresh()
            logger.status("Finished executing plugin ${this::class.simpleName} on unit: $it")
            logger.status(renameEngine.toString())
        }
    }

    override fun getExecutionOptionDefinitions(): List<IOptionDefinition> {
        val out = mutableListOf<IOptionDefinition>()
        if (supportsClassFilter)
            out += ClassFilterOption
        if (defaultForScopeOnThisClass != null)
            out += scopeThisClass(
                defaultForScopeOnThisClass,
                UIBridge.focusedClass?.currentSignature ?: "no class selected"
            )
        if (defaultForScopeOnThisFunction != null)
            out += scopeThisMethod(
                defaultForScopeOnThisFunction,
                UIBridge.focusedMethod?.currentSignature ?: "no method selected"
            )
        return out
    }

    protected abstract fun processUnit(unit: IDexUnit, renameEngine: RenameEngine)
    protected fun processOptions(executionOptions: Map<String, String>) {
        if (supportsClassFilter)
            classFilter = Regex(executionOptions[ClassFilterOptionTag].orIfBlank(ClassFilterDefault))
        if (defaultForScopeOnThisClass != null)
            isOperatingOnlyOnThisClass =
                executionOptions[ScopeThisClassTag].orIfBlank(defaultForScopeOnThisClass.toString()).toBoolean()
        if (defaultForScopeOnThisFunction != null)
            isOperatingOnlyOnThisMethod =
                executionOptions[ScopeThisMethodTag].orIfBlank(defaultForScopeOnThisFunction.toString()).toBoolean()
    }
}