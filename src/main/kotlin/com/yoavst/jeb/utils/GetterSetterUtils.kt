package com.yoavst.jeb.utils

import com.pnfsoftware.jeb.core.units.code.android.IDexUnit
import com.pnfsoftware.jeb.core.units.code.android.dex.IDexClass
import com.pnfsoftware.jeb.core.units.code.android.dex.IDexMethod
import com.pnfsoftware.jeb.core.units.code.java.IJavaAssignment
import com.pnfsoftware.jeb.core.units.code.java.IJavaInstanceField
import com.pnfsoftware.jeb.core.units.code.java.IJavaMethod
import com.pnfsoftware.jeb.core.units.code.java.IJavaReturn
import com.pnfsoftware.jeb.util.logging.GlobalLog
import com.yoavst.jeb.utils.renaming.RenameEngine
import com.yoavst.jeb.utils.renaming.RenameReason
import com.yoavst.jeb.utils.renaming.RenameRequest

private object GetterSetterUtils

private val logger = GlobalLog.getLogger(GetterSetterUtils::class.java)

fun propagateRenameToGetterAndSetters(
    unit: IDexUnit,
    classes: Iterable<IDexClass>,
    renameEngine: RenameEngine,
    useOnlyModified: Boolean = true
) {
    val decompiler = unit.decompiler

    for (cls in classes) {
        logger.trace("Processing for getters/setters: ${cls.currentName}")
        for (method in cls.methods) {
            // Getter / setter should have a verify minimal bytecode, with less than 10 instructions.
            if (method.data.codeItem.instructions.size > 10)
                continue
            val decompiledMethod = decompiler.decompileDexMethod(method) ?: continue
            processPossibleGetterSetter(method, decompiledMethod, cls, renameEngine, useOnlyModified)
        }
    }
}

/** rebuild getter and setters e.g void a(object o){this.a = o;} to void setA(object o); **/
private fun processPossibleGetterSetter(
    method: IDexMethod,
    decompiledMethod: IJavaMethod,
    cls: IDexClass,
    renameEngine: RenameEngine,
    useOnlyModified: Boolean
) {
    if (decompiledMethod.body.size() != 1)
        return
    else if (method.currentName.startsWith("get") || method.currentName.startsWith("set"))
        return

    when (val statement = decompiledMethod.body[0]) {
        is IJavaReturn -> {
            val right = statement.expression
            if (right is IJavaInstanceField) {
                if (right.field == null) {
                    logger.warning("Field is null for ${method.currentSignature}")
                    return
                }

                val name = right.field.originalName
                val currentName = right.field.currentName(cls)
                val (actualCurrentName, renameReason) = when {
                    currentName != null && useOnlyModified -> renameEngine.getModifiedInfo(currentName) ?: return
                    currentName != null && !useOnlyModified -> renameEngine.getModifiedInfo(currentName)
                        ?: name to RenameReason.FieldName
                    currentName == null && useOnlyModified -> return
                    else -> name to RenameReason.FieldName
                }

                renameEngine.renameGetter(
                    RenameRequest(actualCurrentName, renameReason ?: RenameReason.FieldName),
                    method,
                    cls
                )
            }
        }
        is IJavaAssignment -> {
            val left = statement.left
            if (left is IJavaInstanceField) {
                if (left.field == null) {
                    logger.warning("Field is null for ${method.currentSignature}")
                    return
                }

                val name = left.field.originalName
                val currentName = left.field.currentName(cls)
                val (actualCurrentName, renameReason) = when {
                    currentName != null && useOnlyModified -> renameEngine.getModifiedInfo(currentName) ?: return
                    currentName != null && !useOnlyModified -> renameEngine.getModifiedInfo(currentName)
                        ?: name to RenameReason.FieldName
                    currentName == null && useOnlyModified -> return
                    else -> name to RenameReason.FieldName
                }

                renameEngine.renameSetter(
                    RenameRequest(actualCurrentName, renameReason ?: RenameReason.FieldName),
                    method,
                    cls
                )
            }
        }
    }
}