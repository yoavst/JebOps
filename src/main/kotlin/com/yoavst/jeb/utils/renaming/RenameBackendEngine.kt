package com.yoavst.jeb.utils.renaming

import com.pnfsoftware.jeb.core.units.code.android.IDexUnit
import com.pnfsoftware.jeb.core.units.code.android.dex.IDexClass
import com.pnfsoftware.jeb.core.units.code.android.dex.IDexField
import com.pnfsoftware.jeb.core.units.code.android.dex.IDexMethod
import com.pnfsoftware.jeb.core.units.code.java.IJavaField
import com.pnfsoftware.jeb.core.units.code.java.IJavaIdentifier

interface RenameBackendEngine {
    fun renameClass(renameRequest: InternalRenameRequest, cls: IDexClass): Boolean

    fun renameField(renameRequest: InternalRenameRequest, field: IJavaField, cls: IDexClass): Boolean
    fun renameField(renameRequest: InternalRenameRequest, field: IJavaField, unit: IDexUnit): Boolean
    fun renameField(renameRequest: InternalRenameRequest, field: IDexField): Boolean
    fun renameMethod(renameRequest: InternalRenameRequest, method: IDexMethod, cls: IDexClass): Boolean
    fun renameIdentifier(renameRequest: InternalRenameRequest, identifier: IJavaIdentifier, unit: IDexUnit): Boolean
}