package com.yoavst.jeb.plugins.tostring

import com.pnfsoftware.jeb.core.IPluginInformation
import com.pnfsoftware.jeb.core.PluginInformation
import com.pnfsoftware.jeb.core.Version
import com.pnfsoftware.jeb.core.units.code.android.IDexDecompilerUnit
import com.pnfsoftware.jeb.core.units.code.android.IDexUnit
import com.pnfsoftware.jeb.core.units.code.android.dex.IDexClass
import com.pnfsoftware.jeb.core.units.code.java.*
import com.yoavst.jeb.utils.*
import com.yoavst.jeb.utils.renaming.RenameEngine
import com.yoavst.jeb.utils.renaming.RenameReason
import com.yoavst.jeb.utils.renaming.RenameRequest

class ToStringRenamingPlugin : BasicEnginesPlugin(supportsClassFilter = true, defaultForScopeOnThisClass = false) {
    override fun getPluginInformation(): IPluginInformation = PluginInformation(
        "ToString renaming",
        "Fire the plugin to change obfuscated fields' name given a verbose toString implementation",
        "Yoav Sternberg",
        Version.create(0, 1, 0),
        Version.create(3, 0, 16),
        null
    )

    override fun processUnit(unit: IDexUnit, renameEngine: RenameEngine) {
        val decompiler = unit.decompiler
        if (isOperatingOnlyOnThisClass) {
            val cls = focusedClass?.implementingClass ?: return
            processClass(cls, decompiler, renameEngine)
        } else {
            unit.classes.asSequence().filter(classFilter::matches)
                .forEach { processClass(it, decompiler, renameEngine) }
        }

        propagateRenameToGetterAndSetters(unit, renameEngine.stats.effectedClasses, renameEngine)
        unit.refresh()
    }

    private fun processClass(cls: IDexClass, decompiler: IDexDecompilerUnit, renameEngine: RenameEngine) {
        val toStringMethod = cls.getMethod(false, "toString") ?: return
        logger.trace("Processing class: ${cls.currentName}")
        val decompiledToStringMethod = decompiler.decompileDexMethod(toStringMethod) ?: return
        processToStringMethod(decompiledToStringMethod, cls, renameEngine)
    }

    /** Process a toString() method from the given class **/
    private fun processToStringMethod(method: IJavaMethod, cls: IDexClass, renameEngine: RenameEngine) {
        // currently supports only one style of toString
        if (method.body.size() == 0 || method.body[0] !is IJavaReturn)
            return

        var expression = (method.body[0] as IJavaReturn).expression
        while (true) {
            if (expression !is IJavaArithmeticExpression) {
                logger.warn("Warning: The toString method for ${cls.name} is not just an arithmetic expression")
                return
            }

            var left = expression.left
            var right = expression.right

            /*
            2. Remove trailing strings:
               Examples:
                  return "test: " + this.d + " ]"
                  return "Entity [info=" + this.a + "aaa" + "bbb"
               notice exp.getRight() here returns "bbb",
                      exp.getLeft() returns "Entity [info=" + this.a + "aaa"
            */
            while (right is IJavaConstant) {
                // safe because we check it before, and inside the loop
                expression = (expression as IJavaArithmeticExpression).left

                if (expression !is IJavaArithmeticExpression) {
                    logger.debug("Warning: The toString method for ${cls.name} folds to a const toString")
                    return
                }

                left = expression.left
                right = expression.right
            }

            when (left) {
                is IJavaConstant -> {
                    //  we have reached the left end of whole return expression
                    if (!left.isString) {
                        logger.warn("Warning: The toString method for ${cls.name} is not in a valid format, left is not const string")
                        return
                    }

                    val leftRaw = left.string.replaceNonApplicableChars().trim()
                    if (leftRaw.isNotBlank()) {
                        val lefts = leftRaw.split(' ')
                        val fieldName = lefts.last().trim()
                        val firstField = right
                        val possibleClassName = if (lefts.size < 2) null else lefts[0]
                        renamePossibleExpressionWithStr(firstField, fieldName, cls, renameEngine)
                        if (possibleClassName != null) {
                            renameEngine.renameClass(RenameRequest(possibleClassName, RenameReason.ToString), cls)
                        }
                    }
                    break

                }
                is IJavaArithmeticExpression -> {
                    /*
                         2. Left traversal to restore names
                            Example:
                               return "Entity [info=" + this.a + ", value=" + this.b
                               return {"Entity [info="+this.a+}(left.left)"value="(left.right)+this.b
                            In this example:
                               right = this.b
                               left = "Entity [info=" + this.a + ", value="
                               left.right = ", value="
                            So the field name is `sanitize(left.right)`, and the field itself is `right`
                         */
                    val fieldNameAst = left.right
                    if (fieldNameAst !is IJavaConstant || !fieldNameAst.isString) {
                        logger.debug("Warning: The toString method for ${cls.name} is not in a valid format")
                        return
                    }
                    val fieldName = fieldNameAst.string.sanitizeClassname()
                    renamePossibleExpressionWithStr(right, fieldName, cls, renameEngine)
                    // the left traversal in action
                    expression = left.left

                }
                else -> {
                    logger.debug("Warning: The toString method for ${cls.name} has a complex expression ${left.javaClass.canonicalName}")
                    return
                }
            }
        }
    }

    private fun renamePossibleExpressionWithStr(
        exp: IJavaExpression, newName: String,
        cls: IDexClass, renameEngine: RenameEngine
    ) {
        if (newName.isBlank()) {
            logger.debug("Warning: The toString method for ${cls.name} yielded a blank field name")
            return
        } else if (exp is IJavaInstanceField || exp is IJavaStaticField) {
            // why don't they have a common interface god damn it
            val field = (exp as? IJavaInstanceField)?.field ?: (exp as? IJavaStaticField)?.field ?: run {
                logger.info("failed to get field: $exp")
                return
            }
            renameEngine.renameField(RenameRequest(newName, RenameReason.ToString), field, cls)
            return
        } else if (exp is IJavaCall || exp is IJavaConditionalExpression || exp is IJavaPredicate) {
            // support the this.a.ToString() or String.valueOf(this.a) case
            val targetFields = exp.subElements.filterIsInstance(IJavaInstanceField::class.java)
            if (targetFields.size == 1) {
                renamePossibleExpressionWithStr(targetFields[0], newName, cls, renameEngine)
            } else {
                logger.warn("Cannot handle expression: $exp")
            }
            return
        }
        logger.warn("Warning: The toString method for ${cls.name} has a complex expression as a field for $newName: ${exp.javaClass.canonicalName}")
    }


    companion object {
        private fun String.sanitizeClassname() = filter(Char::isJavaIdentifierPart).trim()
        private fun String.replaceNonApplicableChars() = replace(".", "$$").replace("[^0-9a-zA-Z_\$]+".toRegex(), " ")
    }
}