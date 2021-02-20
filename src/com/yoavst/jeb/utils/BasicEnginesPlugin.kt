package com.yoavst.jeb.utils

import com.pnfsoftware.jeb.core.AbstractEnginesPlugin
import com.pnfsoftware.jeb.core.IEnginesContext
import com.pnfsoftware.jeb.core.IOptionDefinition
import com.pnfsoftware.jeb.core.units.code.android.IDexUnit
import com.pnfsoftware.jeb.util.logging.GlobalLog
import com.pnfsoftware.jeb.util.logging.ILogger
import com.yoavst.jeb.tostring.getDexUnits
import com.yoavst.jeb.utils.renaming.RenameEngine

abstract class BasicEnginesPlugin(private val supportsClassFilter: Boolean = false) : AbstractEnginesPlugin() {
    protected lateinit var context: IEnginesContext
    protected val logger: ILogger = GlobalLog.getLogger(javaClass)

    protected lateinit var classFilter: Regex

    init {
        logToFile()
    }

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
        return out
    }

    protected abstract fun processUnit(unit: IDexUnit, renameEngine: RenameEngine)
    protected fun processOptions(executionOptions: Map<String, String>) {
        if (supportsClassFilter)
            classFilter = Regex(executionOptions[ClassFilterOption.name].orIfBlank(ClassFilterDefault))
    }
}