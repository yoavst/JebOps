package com.yoavst.jeb.plugins.enumsupport

import com.pnfsoftware.jeb.core.IPluginInformation
import com.pnfsoftware.jeb.core.PluginInformation
import com.pnfsoftware.jeb.core.units.code.android.IDexUnit
import com.pnfsoftware.jeb.core.units.code.android.dex.IDexClass
import com.pnfsoftware.jeb.core.units.code.android.dex.IDexMethod
import com.yoavst.jeb.bridge.UIBridge
import com.yoavst.jeb.plugins.JEB_VERSION
import com.yoavst.jeb.plugins.PLUGIN_VERSION
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
        PLUGIN_VERSION,
        JEB_VERSION,
        null
    )

    override fun processUnit(unit: IDexUnit, renameEngine: RenameEngine) {
        if (isOperatingOnlyOnThisClass) {
            val cls = UIBridge.focusedClass?.implementingClass ?: return
            processClass(cls, renameEngine)
        } else {
            unit.subclassesOf("Ljava/lang/Enum;").filter(classFilter::matches)
                .forEach { processClass(it, renameEngine) }
        }
        unit.refresh()
    }


    private fun processClass(cls: IDexClass, renameEngine: RenameEngine) {
        logger.trace("Processing enum: $cls")
        val staticConstructor = cls.methods.firstOrNull { it.originalName == "<clinit>" } ?: run {
            logger.info("Enum without static initializer: ${cls.name}")
            return
        }
        val constructors = cls.methods.filter { it.originalName == "<init>" }

        if (constructors.isEmpty()) {
            logger.info("Enum without constructor: ${cls.name}")
            return
        }


        if (constructors.size == 1 && constructors[0].parameterTypes.size == 0) {
            // Enum with an empty constructor, Therefore it has at most one instance
            processSingletonEnum(cls, constructors[0], renameEngine)
        } else {
            if (constructors.any { it.parameterTypes.size == 0 || it.parameterTypes[0].signature != "Ljava/lang/String;" }) {
                if (constructors.size == 1) {
                    // Assume it is a singleton enum
                    processSingletonEnum(cls, constructors[0], renameEngine)
                } else {
                    logger.warning("Normal Enum with constructor receiving non string type at first index: ${cls.name}")
                }
            } else {
                // Normal enum
                MultiEnumStaticConstructorSimulator(cls, constructors, renameEngine).run(staticConstructor)
            }
        }

        renameEngine.renameClass(RenameRequest("Enum", RenameReason.Type, informationalRename = true), cls)
    }

    fun processSingletonEnum(clazz: IDexClass, constructor: IDexMethod, renameEngine: RenameEngine) {
        val superMethod = constructor.dex.getMethod("Ljava/lang/Enum;-><init>(Ljava/lang/String;I)V")

        val simulator = SingleEnumConstructorSimulator(clazz, setOf(superMethod.prototypeIndex), renameEngine)
        simulator.run(constructor)

        if (simulator.name != null) {
            // Find enum field
            val matchingFields = clazz.fields.filter { it.fieldTypeIndex == clazz.classTypeIndex }
            if (matchingFields.size != 1) {
                logger.warning("Found multiple matching field in ${clazz.name}. Cannot rename to ${simulator.name}")
            } else {
                renameEngine.renameField(RenameRequest(simulator.name!!, RenameReason.EnumName), matchingFields[0], clazz)
            }
        } else {
            logger.info("Could not rename singleton enum: ${clazz.name}")
        }
    }
}