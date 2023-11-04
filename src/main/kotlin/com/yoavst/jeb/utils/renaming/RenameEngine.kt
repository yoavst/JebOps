package com.yoavst.jeb.utils.renaming

import com.pnfsoftware.jeb.core.units.code.android.IDexUnit
import com.pnfsoftware.jeb.core.units.code.android.dex.IDexClass
import com.pnfsoftware.jeb.core.units.code.android.dex.IDexField
import com.pnfsoftware.jeb.core.units.code.android.dex.IDexMethod
import com.pnfsoftware.jeb.core.units.code.java.IJavaField
import com.pnfsoftware.jeb.core.units.code.java.IJavaIdentifier

interface RenameEngine {
    val stats: RenameStats

    fun renameClass(renameRequest: RenameRequest, cls: IDexClass)
    fun renameField(renameRequest: RenameRequest, field: IJavaField, cls: IDexClass)
    fun renameField(renameRequest: RenameRequest, field: IDexField, cls: IDexClass)
    fun renameMethod(renameRequest: RenameRequest, method: IDexMethod, cls: IDexClass)
    fun renameGetter(renameRequest: RenameRequest, method: IDexMethod, cls: IDexClass)
    fun renameSetter(renameRequest: RenameRequest, method: IDexMethod, cls: IDexClass)
    fun renameIdentifier(renameRequest: RenameRequest, identifier: IJavaIdentifier, unit: IDexUnit)

    fun getModifiedInfo(name: String): Pair<String, RenameReason?>?

    companion object {
        fun create() = RenameEngineImpl(RenameFrontendEngineImpl, RenameBackendEngineImpl)
    }
}