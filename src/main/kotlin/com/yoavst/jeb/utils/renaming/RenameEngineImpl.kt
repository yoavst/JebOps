package com.yoavst.jeb.utils.renaming

import com.pnfsoftware.jeb.core.units.code.android.IDexUnit
import com.pnfsoftware.jeb.core.units.code.android.dex.IDexClass
import com.pnfsoftware.jeb.core.units.code.android.dex.IDexField
import com.pnfsoftware.jeb.core.units.code.android.dex.IDexMethod
import com.pnfsoftware.jeb.core.units.code.java.IJavaField
import com.pnfsoftware.jeb.core.units.code.java.IJavaIdentifier
import com.yoavst.jeb.utils.currentName
import java.util.*

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
            stats.effectedClasses.add(cls)
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
            stats.renamedFields[cls.dex.getField(field.signature)] = renameRequest
            stats.effectedClasses.add(cls)
        }
    }

    override fun renameField(renameRequest: RenameRequest, field: IDexField, cls: IDexClass) {
        val name = field.currentName
        val internalRenameRequest = InternalRenameRequest.ofField(
                name,
                renameRequest.newName,
                renameRequest.reason,
                renameRequest.informationalRename
        )
        val finalRenameRequest = frontendEngine.applyRules(internalRenameRequest) ?: return
        if (backendEngine.renameField(finalRenameRequest, field)) {
            stats.renamedFields[field] = renameRequest
            stats.effectedClasses.add(cls)
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
            stats.effectedClasses.add(cls)
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
            // no class is effected since it is identifier
        }
    }

    override fun getModifiedInfo(name: String) = frontendEngine.getModifiedInfo(name)

    override fun renameGetter(renameRequest: RenameRequest, method: IDexMethod, cls: IDexClass) {
        val preInternalNameRequest = InternalRenameRequest.ofIdentifier(
                method.currentName,
                renameRequest.newName,
                renameRequest.reason,
                renameRequest.informationalRename
        )
        frontendEngine.applyRules(preInternalNameRequest) ?: return
        // now for the real request:
        val internalNameRequest = InternalRenameRequest.ofIdentifier(
                method.currentName,
                "get" + renameRequest.newName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
                renameRequest.reason,
                renameRequest.informationalRename
        )
        val finalRequest = frontendEngine.applyRules(internalNameRequest) ?: return
        if (backendEngine.renameMethod(finalRequest, method, cls)) {
            stats.renamedMethods[method] = renameRequest
            stats.effectedClasses.add(cls)
        }
    }

    override fun renameSetter(renameRequest: RenameRequest, method: IDexMethod, cls: IDexClass) {
        val preInternalNameRequest = InternalRenameRequest.ofIdentifier(
                method.currentName,
                renameRequest.newName,
                renameRequest.reason,
                renameRequest.informationalRename
        )
        frontendEngine.applyRules(preInternalNameRequest) ?: return
        // now for the real request:
        val internalNameRequest = InternalRenameRequest.ofIdentifier(
                method.currentName,
                "set" + renameRequest.newName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
                renameRequest.reason,
                renameRequest.informationalRename
        )
        val finalRequest = frontendEngine.applyRules(internalNameRequest) ?: return
        if (backendEngine.renameMethod(finalRequest, method, cls)) {
            stats.renamedMethods[method] = renameRequest
            stats.effectedClasses.add(cls)
        }
    }

    override fun toString(): String = stats.toString()
}