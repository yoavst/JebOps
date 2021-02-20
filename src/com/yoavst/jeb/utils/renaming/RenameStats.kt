package com.yoavst.jeb.utils.renaming

import com.pnfsoftware.jeb.core.units.code.android.dex.IDexClass
import com.pnfsoftware.jeb.core.units.code.android.dex.IDexMethod
import com.pnfsoftware.jeb.core.units.code.java.IJavaField
import com.pnfsoftware.jeb.core.units.code.java.IJavaIdentifier

class RenameStats {
    val renamedClasses: MutableMap<IDexClass, RenameRequest> = mutableMapOf()
    val renamedMethods: MutableMap<IDexMethod, RenameRequest> = mutableMapOf()
    val renamedFields: MutableMap<IJavaField, RenameRequest> = mutableMapOf()
    val renamedIdentifiers: MutableMap<IJavaIdentifier, RenameRequest> = mutableMapOf()

    fun clear() {
        renamedClasses.clear()
        renamedMethods.clear()
        renamedFields.clear()
        renamedIdentifiers.clear()
    }

    override fun toString(): String = buildString {
        appendLine("Stats:")
        if (renamedClasses.isNotEmpty()) {
            append(" Classes: ${renamedClasses.size}")
        }
        if (renamedMethods.isNotEmpty()) {
            appendLine(" Methods: ${renamedMethods.size}")
        }
        if (renamedFields.isNotEmpty()) {
            append(" Fields: ${renamedFields.size}")
        }
        if (renamedIdentifiers.isNotEmpty()) {
            append(" Identifiers: ${renamedIdentifiers.size}")
        }
    }
}