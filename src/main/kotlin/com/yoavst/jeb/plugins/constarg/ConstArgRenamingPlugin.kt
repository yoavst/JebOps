package com.yoavst.jeb.plugins.constarg

import com.pnfsoftware.jeb.core.*
import com.pnfsoftware.jeb.core.units.code.android.IDexDecompilerUnit
import com.pnfsoftware.jeb.core.units.code.android.IDexUnit
import com.pnfsoftware.jeb.core.units.code.android.dex.IDexClass
import com.pnfsoftware.jeb.core.units.code.android.dex.IDexMethod
import com.pnfsoftware.jeb.core.units.code.java.*
import com.yoavst.jeb.bridge.UIBridge
import com.yoavst.jeb.utils.*
import com.yoavst.jeb.utils.renaming.RenameEngine
import com.yoavst.jeb.utils.renaming.RenameReason
import com.yoavst.jeb.utils.renaming.RenameRequest
import kotlin.properties.Delegates

class ConstArgRenamingPlugin :
    BasicEnginesPlugin(
        supportsClassFilter = true,
        defaultForScopeOnThisClass = false,
        defaultForScopeOnThisFunction = false,
        usingSelectedMethod = true
    ) {

    private var constArgumentIndex by Delegates.notNull<Int>()
    private var renamedArgumentIndex by Delegates.notNull<Int>()
    private lateinit var renameMethod: IDexMethod
    private lateinit var renamer: (String) -> RenameResult
    private var effectedMethods: MutableMap<IJavaMethod, IDexClass> = mutableMapOf()

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
        renamer = when (RenameTarget.valueOf(targetRenameStr)) {
            RenameTarget.Custom -> {
                val tmp = executionOptions[TargetArgumentToBeRenamedPosition]?.toIntOrNull()
                if (tmp != null)
                    renamedArgumentIndex = tmp

                val script = displayFileOpenSelector("Renaming script") ?: run {
                    logger.error("No script selected")
                    return false
                }
                scriptRenamer(script)
            }
            RenameTarget.Class -> classRenamer
            RenameTarget.Method -> methodRenamer
            RenameTarget.Argument -> {
                renamedArgumentIndex = executionOptions[TargetArgumentToBeRenamedPosition]?.toIntOrNull() ?: run {
                    logger.error("Index of the renamed argument be a number")
                    return false
                }
                if (constArgumentIndex < 0 || renameMethod.parameterTypes.size <= constArgumentIndex) {
                    logger.error("The renamed argument index is out of range: [0, ${renameMethod.parameterTypes.size})")
                    return false
                }
                argumentRenamer
            }
            RenameTarget.Asignee -> {
                asigneeRenamer
            }
        }
        return true
    }

    override fun processUnit(unit: IDexUnit, renameEngine: RenameEngine) {
        effectedMethods.clear()

        val decompiler = unit.decompiler
        if (isOperatingOnlyOnThisMethod) {
            if (UIBridge.currentMethod != null && UIBridge.currentClass != null) {
                // you cannot see the sources of a type without implementing class
                processMethod(UIBridge.currentMethod!!, unit, decompiler, renameEngine)
            }
        } else {
            val methods = unit.xrefsFor(renameMethod).mapTo(mutableSetOf(), unit::getMethod)
            var seq = methods.asSequence().filter { classFilter.matches(it.classType.implementingClass) }
            if (isOperatingOnlyOnThisClass) {
                seq = seq.filter { it.classType == UIBridge.currentClass }
            }

            seq.forEach { processMethod(it, unit, decompiler, renameEngine) }

        }

        effectedMethods.forEach { (method, cls) ->
            SimpleIdentifierPropagationTraversal(cls, renameEngine).traverse(method.body)
        }

        propagateRenameToGetterAndSetters(unit, renameEngine.stats.effectedClasses, renameEngine)
        unit.refresh()
    }

    private fun processMethod(
        method: IDexMethod, unit: IDexUnit, decompiler: IDexDecompilerUnit,
        renameEngine: RenameEngine
    ) {
        val decompiledMethod = decompiler.decompileDexMethod(method) ?: run {
            logger.warning("Failed to decompile method: ${method.currentSignature}")
            return
        }
        ConstArgRenamingTraversal(
            method,
            decompiledMethod,
            method.classType.implementingClass!!,
            unit,
            renameEngine
        ).traverse(decompiledMethod.body)
    }

    private inner class ConstArgRenamingTraversal(
        private val method: IDexMethod,
        private val javaMethod: IJavaMethod,
        private val cls: IDexClass,
        private val unit: IDexUnit,
        renameEngine: RenameEngine
    ) :
        BasicAstTraversal(renameEngine) {
        private val renamedSignature: String = renameMethod.getSignature(false)
        override fun traverseNonCompound(statement: IStatement) {
            if (statement is IJavaAssignment) {
                traverseElement(statement.right, statement.left)
            } else {
                traverseElement(statement, null)
            }
        }

        private fun traverseElement(element: IJavaElement, asignee: IJavaLeftExpression? = null): Unit = when {
            element is IJavaCall && element.methodSignature == renamedSignature -> {
                // we found the method we were looking for!
                processMatchedMethod(asignee, element::getRealArgument)
            }
            element is IJavaNew && element.constructor.signature == renamedSignature -> {
                // the method we were looking for was a constructor
                processMatchedMethod(asignee) { element.arguments[it] }
            }
            else -> {
                // Try sub elements
                element.subElements.forEach { traverseElement(it, asignee) }
            }
        }

        private inline fun processMatchedMethod(asignee: IJavaLeftExpression?, getArg: (Int) -> IJavaElement) {
            val nameArg = getArg(constArgumentIndex)
            if (nameArg is IJavaConstant && nameArg.isString) {
                val result = renamer(nameArg.string)
                if (!result.className.isNullOrEmpty()) {
                    renameEngine.renameClass(
                        RenameRequest(
                            result.className,
                            RenameReason.MethodStringArgument
                        ), cls
                    )
                }
                if (!result.methodName.isNullOrEmpty()) {
                    renameEngine.renameMethod(
                        RenameRequest(
                            result.methodName,
                            RenameReason.MethodStringArgument
                        ), method, cls
                    )
                }
                if (!result.argumentName.isNullOrEmpty()) {
                    renameElement(getArg(renamedArgumentIndex), result.argumentName)
                }
                if (!result.assigneeName.isNullOrEmpty() && asignee != null) {
                    renameElement(asignee, result.assigneeName)
                }
            }
        }

        private fun renameElement(element: IJavaElement, name: String) {
            val request = RenameRequest(name, RenameReason.MethodStringArgument)
            when (element) {
                is IJavaDefinition -> renameElement(element.identifier, name)
                is IJavaStaticField -> {
                    val field = element.field ?: run {
                        logger.warning("Failed to get field: $element")
                        return
                    }
                    renameEngine.renameField(request, field, cls)
                }
                is IJavaInstanceField -> {
                    val field = element.field ?: run {
                        logger.warning("Failed to get field: $element")
                        return
                    }
                    renameEngine.renameField(request, field, cls)
                }
                is IJavaIdentifier -> {
                    renameEngine.renameIdentifier(request, element, unit)
                }
                else -> {
                    logger.debug("Unsupported argument type: ${element.elementType}")
                    return
                }
            }
            effectedMethods[javaMethod] = cls
        }
    }

    /**
     * We are going to do very simple "Identifier propagation", to support the case of:
    this.a = anIdentifierIRecovered
     */
    private inner class SimpleIdentifierPropagationTraversal(private val cls: IDexClass, renameEngine: RenameEngine) :
        BasicAstTraversal(renameEngine) {
        override fun traverseNonCompound(statement: IStatement) {
            if (statement is IJavaAssignment) {
                val left = statement.left
                val right = statement.right

                if (right is IJavaIdentifier) {
                    val renameRequest = renameEngine.stats.renamedIdentifiers[right] ?: return
                    if (left is IJavaInstanceField) {
                        val field = left.field ?: run {
                            logger.warning("Failed to get field: $left")
                            return
                        }
                        renameEngine.renameField(
                            RenameRequest(
                                renameRequest.newName,
                                RenameReason.MethodStringArgument
                            ), field, cls
                        )
                    } else if (left is IJavaStaticField) {
                        val field = left.field ?: run {
                            logger.warning("Failed to get field: $left")
                            return
                        }
                        renameEngine.renameField(
                            RenameRequest(
                                renameRequest.newName,
                                RenameReason.MethodStringArgument
                            ), field, cls
                        )
                    }
                }
            }
        }

    }


    companion object {
        private const val TargetRenameTag = "Target rename"
        private const val TargetConstArgIndex = "Const arg index"
        private const val TargetArgumentToBeRenamedPosition = "renamed arg index"
    }
}