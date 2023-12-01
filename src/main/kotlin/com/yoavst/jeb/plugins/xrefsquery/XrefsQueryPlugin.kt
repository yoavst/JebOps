package com.yoavst.jeb.plugins.xrefsquery

import com.pnfsoftware.jeb.core.IPluginInformation
import com.pnfsoftware.jeb.core.PluginInformation
import com.pnfsoftware.jeb.core.units.code.android.IDexUnit
import com.pnfsoftware.jeb.core.units.code.android.dex.IDexMethod
import com.pnfsoftware.jeb.core.units.code.java.*
import com.yoavst.jeb.bridge.UIBridge
import com.yoavst.jeb.plugins.JEB_VERSION
import com.yoavst.jeb.plugins.PLUGIN_VERSION
import com.yoavst.jeb.plugins.constarg.scriptRenamer
import com.yoavst.jeb.utils.*
import com.yoavst.jeb.utils.renaming.RenameEngine
import java.io.File

class XrefsQueryPlugin :
    BasicEnginesPlugin(supportsClassFilter = false, usingSelectedMethod = true) {
    private lateinit var script: String
    override fun getPluginInformation(): IPluginInformation = PluginInformation(
        "Xrefs Query",
        "Used this plugin to filter xrefs",
        "Yoav Sternberg",
        PLUGIN_VERSION,
        JEB_VERSION,
        null
    )

    override fun processOptions(executionOptions: Map<String, String>): Boolean {
        super.processOptions(executionOptions)
        val scriptPath = displayFileOpenSelector("xrefs script") ?: run {
            logger.error("No script selected")
            return false
        }
        script = File(scriptPath).readText()
        return true
    }

    override fun processUnit(unit: IDexUnit, renameEngine: RenameEngine) {
        val expectedMethod = UIBridge.focusedMethod
        if (expectedMethod == null) {
            logger.warning("Selected method is null")
            return
        }

        logger.info("Using method: ${expectedMethod.signature}")

        val decompiler = unit.decompilerRef
        val xrefMethods = unit.xrefsFor(expectedMethod)
            .mapTo(mutableSetOf(), unit::getMethod)

        logger.info("Xref methods: ${xrefMethods.size}")

        xrefMethods.parallelStream()
            .map { decompiler.decompileDexMethod(it)?.let { javaMethod -> it to javaMethod } }
            .filter { it != null }
            .sequential()
            .forEach { (dexMethod, javaMethod) ->
                processMethod(
                    expectedMethod,
                    dexMethod,
                    javaMethod,
                    renameEngine,
                    script
                )
            }
    }

    private fun processMethod(
        expectedMethod: IDexMethod,
        dexMethod: IDexMethod,
        javaMethod: IJavaMethod,
        renameEngine: RenameEngine,
        script: String
    ) {
        ArgsTraversal(expectedMethod, dexMethod, javaMethod, renameEngine, script).traverse(javaMethod)
    }

    private inner class ArgsTraversal(
        private val expectedMethod: IDexMethod,
        private val dexMethod: IDexMethod,
        private val javaMethod: IJavaMethod,
        renameEngine: RenameEngine,
        private val script: String
    ) :
        BasicAstTraversal(renameEngine) {

        private val expectedSig = expectedMethod.getSignature(false)
        private val argsCount = expectedMethod.parameterTypes.size


        override fun traverseNonCompound(statement: IJavaStatement) {
            if (statement is IJavaAssignment) {
                // Don't crash on: "Object x;"
                statement.right?.let(::traverseElement)
            } else {
                traverseElement(statement)
            }
        }

        private fun traverseElement(element: IJavaElement): Unit = when {
            element is IJavaCall && element.methodSignature == expectedSig -> {
                // we found the method we were looking for!
                try {
                    processMatchedMethod(element, element::getRealArgument)
                } catch (e: Exception) {
                    logger.error("Failed for ${element.methodSignature}")
                    throw e
                }
            }

            element is IJavaNew && element.constructorSignature == expectedSig -> {
                // the method we were looking for was a constructor
                processMatchedMethod(element) { element.arguments[it] }
            }

            else -> {
                // Try sub elements
                element.subElements.forEach(::traverseElement)
            }
        }

        private inline fun processMatchedMethod(
            element: IJavaElement,
            getArg: (Int) -> IJavaElement
        ) {
            val args = List(argsCount, getArg)
            val isExpected = JythonHelper.execute(
                script, ExecuteParams(
                    expectedMethod,
                    javaMethod,
                    dexMethod,
                    args,
                    element
                )
            )

            if (isExpected) {
                logger.info("$dexMethod+${element.physicalOffset.toString(16)}h")
            }
        }
    }
}