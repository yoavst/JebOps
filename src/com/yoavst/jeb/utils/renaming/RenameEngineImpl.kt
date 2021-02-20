package com.yoavst.jeb.utils.renaming

import com.pnfsoftware.jeb.core.units.code.android.IDexUnit
import com.pnfsoftware.jeb.core.units.code.android.dex.IDexClass
import com.pnfsoftware.jeb.core.units.code.android.dex.IDexMethod
import com.pnfsoftware.jeb.core.units.code.java.IJavaField
import com.pnfsoftware.jeb.core.units.code.java.IJavaIdentifier
import com.yoavst.jeb.utils.currentName

class RenameEngineImpl(
    private val frontendEngine: RenameFrontendEngine,
    private val backendEngine: RenameBackendEngine
) : RenameEngine {
    override val stats: RenameStats = RenameStats()
    override fun renameClass(renameRequest: RenameRequest, cls: IDexClass) {
        val internalRenameRequest = InternalRenameRequest.ofClass(
            cls.currentName,
            renameRequest.newName,
            renameRequest.reason,
            renameRequest.informationalRename
        )
        val finalRenameRequest = frontendEngine.applyRules(internalRenameRequest) ?: return
        if (backendEngine.renameClass(finalRenameRequest, cls)) {
            stats.renamedClasses[cls] = renameRequest
        }
    }

    override fun renameField(renameRequest: RenameRequest, field: IJavaField, cls: IDexClass) {
        val name = field.currentName(cls) ?: return
        val internalRenameRequest = InternalRenameRequest.ofField(
            name,
            renameRequest.newName,
            renameRequest.reason,
            renameRequest.informationalRename
        )
        val finalRenameRequest = frontendEngine.applyRules(internalRenameRequest) ?: return
        if (backendEngine.renameField(finalRenameRequest, field, cls)) {
            stats.renamedFields[field] = renameRequest
        }
    }

    override fun renameField(renameRequest: RenameRequest, field: IJavaField, unit: IDexUnit) {
        val name = field.currentName(unit) ?: return
        val internalRenameRequest = InternalRenameRequest.ofField(
            name,
            renameRequest.newName,
            renameRequest.reason,
            renameRequest.informationalRename
        )
        val finalRenameRequest = frontendEngine.applyRules(internalRenameRequest) ?: return
        if (backendEngine.renameField(finalRenameRequest, field, unit)) {
            stats.renamedFields[field] = renameRequest
        }
    }

    override fun renameMethod(renameRequest: RenameRequest, method: IDexMethod, cls: IDexClass) {
        val internalRenameRequest = InternalRenameRequest.ofMethod(
            method.currentName,
            renameRequest.newName,
            renameRequest.reason,
            renameRequest.informationalRename
        )
        val finalRenameRequest = frontendEngine.applyRules(internalRenameRequest) ?: return
        if (backendEngine.renameMethod(finalRenameRequest, method, cls)) {
            stats.renamedMethods[method] = renameRequest
        }
    }

    override fun renameIdentifier(renameRequest: RenameRequest, identifier: IJavaIdentifier, unit: IDexUnit) {
        val internalRenameRequest = InternalRenameRequest.ofIdentifier(
            identifier.currentName(unit),
            renameRequest.newName,
            renameRequest.reason,
            renameRequest.informationalRename
        )
        val finalRenameRequest = frontendEngine.applyRules(internalRenameRequest) ?: return
        if (backendEngine.renameIdentifier(finalRenameRequest, identifier, unit)) {
            stats.renamedIdentifiers[identifier] = renameRequest
        }
    }
}