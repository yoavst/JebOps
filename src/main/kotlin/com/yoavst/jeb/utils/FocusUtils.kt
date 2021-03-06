package com.yoavst.jeb.utils

import com.pnfsoftware.jeb.client.api.IGraphicalClientContext
import com.pnfsoftware.jeb.core.output.ItemClassIdentifiers
import com.pnfsoftware.jeb.core.output.code.AssemblyItem
import com.pnfsoftware.jeb.core.units.IAddressableUnit
import com.pnfsoftware.jeb.core.units.code.ICodeUnit
import com.pnfsoftware.jeb.core.units.code.android.IDexDecompilerUnit
import com.pnfsoftware.jeb.core.units.code.android.dex.IDexClass
import com.pnfsoftware.jeb.core.units.code.android.dex.IDexMethod
import com.pnfsoftware.jeb.core.units.code.android.dex.IDexType
import com.pnfsoftware.jeb.core.units.code.java.IJavaSourceUnit
import com.pnfsoftware.jeb.util.logging.GlobalLog
import com.pnfsoftware.jeb.util.logging.ILogger

private class FocusUtils

private val logger: ILogger = GlobalLog.getLogger(FocusUtils::class.java)

fun IGraphicalClientContext.currentFocusedMethod(supportFocus: Boolean = true, verbose: Boolean = true): IDexMethod? {
    val fragment = focusedFragment
    var address = fragment?.activeAddress
    if (fragment == null || address == null) {
        if (verbose) {
            logger.error(
                "Set the focus on a UI fragment, and position the caret somewhere in the method you would like to work on\n" +
                        "Note: If you focus it on method invocation, it is going to be that method and not the container method."
            )
        }
        return null
    }

    var unit = fragment.unit
    if (unit is IJavaSourceUnit) {
        val parent = unit.parent
        if (parent !is IDexDecompilerUnit) {
            logger.error("Parent for java source is not dex decompiler unit: $parent")
            return null
        }
        unit = parent
    } else if (unit !is ICodeUnit) {
        logger.error("Unit is not supported: $unit")
        return null
    }
    // get compiler to know that unit is addressable
    if (unit !is IAddressableUnit) {
        logger.error("Unit is not addressable: $unit")
        return null
    }

    // try selected item first
    if (supportFocus) {
        val item = fragment.activeItem
        if (item is AssemblyItem && item.classId == ItemClassIdentifiers.METHOD_NAME) {
            val selectedMethod = unit.getItemObject(item.itemId)
            if (selectedMethod == null) {
                logger.error("Cannot get selected method: $fragment")
                return null
            } else if (selectedMethod !is IDexMethod) {
                logger.error("Selected method is not dex: $selectedMethod")
                return null
            }
            return selectedMethod
        }
    }

    // fallback to select current method

    if (unit is IDexDecompilerUnit) {
        val pos = address.indexOf('+')
        if (pos >= 0) {
            address = address.substring(0, pos)
        }
        val javaMethod = unit.getMethod(address, false)
        if (javaMethod == null) {
            logger.error("Not inside a method: $address")
            return null
        }
        val options = enginesContext.getDexUnits().mapNotNull(javaMethod::toDexMethod)
        if (options.isEmpty()) {
            return null
        }
        return options[0]
    } else {
        val method = (unit as ICodeUnit).getMethod(address)
        if (method == null) {
            logger.error("Not a method at the given address: $address")
            return null
        }

        if (method is IDexMethod)
            return method

        logger.error("Method is of invalid class: $method")
        return null
    }
}

fun IGraphicalClientContext.currentFocusedType(supportFocus: Boolean = true, verbose: Boolean = true): IDexType? {
    val fragment = focusedFragment
    var address = fragment?.activeAddress
    if (fragment == null || address == null) {
        if (verbose) {
            logger.error(
                "Set the focus on a UI fragment, and position the caret somewhere in the class (inside method) you would like to work on\n" +
                        "Note: If you focus it on a class name, it is going to be that class and not the container class."
            )
        }
        return null
    }

    var unit = fragment.unit
    if (unit is IJavaSourceUnit) {
        val parent = unit.parent
        if (parent !is IDexDecompilerUnit) {
            logger.error("Parent for java source is not dex decompiler unit: $parent")
            return null
        }
        unit = parent
    } else if (unit !is ICodeUnit) {
        logger.error("Unit is not supported: $unit")
        return null
    }
    // get compiler to know that unit is addressable
    if (unit !is IAddressableUnit) {
        logger.error("Unit is not addressable: $unit")
        return null
    }

    // try selected item first
    if (supportFocus) {
        val item = fragment.activeItem
        if (item is AssemblyItem) {
            if (item.classId == ItemClassIdentifiers.CLASS_NAME) {
                return when (val selectedClass = unit.getItemObject(item.itemId)) {
                    null -> {
                        logger.error("Cannot get selected class: $fragment")
                        null
                    }
                    is IDexClass -> selectedClass.classType as? IDexType
                    else -> {
                        logger.error("Selected class is not dex: $selectedClass")
                        null
                    }
                }
            } else if (item.classId == ItemClassIdentifiers.EXTERNAL_CLASS_NAME) {
                return when (val selectedType = unit.getItemObject(item.itemId)) {
                    null -> {
                        logger.error("Cannot get selected type: $fragment")
                        null
                    }
                    is IDexType -> selectedType
                    else -> {
                        logger.error("Selected type is not dex: $selectedType")
                        null
                    }
                }
            }
        }
    }

    // fallback to select current class

    if (unit is IDexDecompilerUnit) {
        val pos = address.indexOf("->")
        if (pos >= 0) {
            address = address.substring(0, pos)
        }
        val javaClass = unit.getClass(address, false)
        if (javaClass == null) {
            logger.error("Not inside class: $address")
            return null
        }
        val options =
            enginesContext.getDexUnits()
                .mapNotNull { dexUnit -> dexUnit.types.firstOrNull { it.originalSignature == javaClass.signature } }
        if (options.isEmpty()) {
            return null
        }
        return options[0]
    } else {
        val pos = address.indexOf("->")
        if (pos >= 0) {
            address = address.substring(0, pos)
        }
        val dexClass = (unit as ICodeUnit).getClass(address)
        if (dexClass == null) {
            logger.error("Not inside class: $address")
            return null
        }
        return dexClass.classType as IDexType
    }
}