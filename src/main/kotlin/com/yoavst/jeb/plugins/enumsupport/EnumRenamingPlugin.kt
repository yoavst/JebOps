package com.yoavst.jeb.plugins.enumsupport

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

/**
 * Refactor assignments of the given forms:
```
// 1.
cls.a = new EnumCls("LIST_TYPE", ...)
// 2.
var temp = new EnumCls("LIST_TYPE", ...)
cls.a = temp
```
 */
class EnumRenamingPlugin : BasicEnginesPlugin(supportsClassFilter = true, defaultForScopeOnThisClass = false) {
    override fun getPluginInformation(): IPluginInformation = PluginInformation(
        "Enum fields renaming",
        "Fire the plugin to change obfuscated enum field names to their real name if available",
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
            unit.subclassesOf("Ljava/lang/Enum;").filter(classFilter::matches)
                .forEach { processClass(it, decompiler, renameEngine) }
        }
        unit.refresh()
    }

    private fun processClass(cls: IDexClass, decompiler: IDexDecompilerUnit, renameEngine: RenameEngine) {
        logger.trace("Processing enum: $cls")
        val staticConstructor = cls.methods.first { it.originalName == "<clinit>" }
        val decompiledStaticConstructor = decompiler.decompileDexMethod(staticConstructor) ?: return
        EnumAstTraversal(cls, renameEngine).traverse(decompiledStaticConstructor)
        renameEngine.renameClass(RenameRequest("Enum", RenameReason.Type, informationalRename = true), cls)
    }

    private class EnumAstTraversal(private val cls: IDexClass, renameEngine: RenameEngine) :
        BasicAstTraversal(renameEngine) {
        private val classSignature: String = cls.originalSignature
        private val mapping = mutableMapOf<IJavaIdentifier, IJavaNew>()

        override fun traverseNonCompound(statement: IStatement) {
            if (statement !is IJavaAssignment)
                return

            val right = when (val originalRight = statement.right) {
                is IJavaNew -> originalRight
                is IJavaIdentifier -> mapping.getOrElse(originalRight) { return }
                else -> return
            }

            when (val left = statement.left) {
                is IJavaIdentifier -> {
                    mapping[left] = right
                }
                is IJavaDefinition -> {
                    if (left.type.signature == classSignature)
                        mapping[left.identifier] = right
                }
                is IJavaStaticField -> {
                    if (right.type.signature != classSignature)
                        return

                    val constString = right.arguments.firstOrNull { it is IJavaConstant && it.isString }
                    if (constString != null) {
                        val newName = (constString as IJavaConstant).string
                        renameEngine.renameField(
                            RenameRequest(newName, RenameReason.EnumName),
                            left.field ?: return,
                            cls
                        )
                    }
                }
            }
        }
    }
}