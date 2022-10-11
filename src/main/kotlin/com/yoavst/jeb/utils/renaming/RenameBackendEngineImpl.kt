package com.yoavst.jeb.utils.renaming

import com.pnfsoftware.jeb.core.units.code.android.IDexDecompilerUnit
import com.pnfsoftware.jeb.core.units.code.android.IDexUnit
import com.pnfsoftware.jeb.core.units.code.android.dex.IDexClass
import com.pnfsoftware.jeb.core.units.code.android.dex.IDexField
import com.pnfsoftware.jeb.core.units.code.android.dex.IDexMethod
import com.pnfsoftware.jeb.core.units.code.java.IJavaField
import com.pnfsoftware.jeb.core.units.code.java.IJavaIdentifier
import com.pnfsoftware.jeb.util.logging.GlobalLog
import com.yoavst.jeb.utils.currentName
import com.yoavst.jeb.utils.decompilerRef
import com.yoavst.jeb.utils.originalName
import com.yoavst.jeb.utils.originalSignature

object RenameBackendEngineImpl : RenameBackendEngine {
    private val logger = GlobalLog.getLogger(javaClass)
    private val decompilerCache: MutableMap<IDexUnit, IDexDecompilerUnit> = mutableMapOf()

    override fun renameClass(renameRequest: InternalRenameRequest, cls: IDexClass): Boolean {
        if (cls.setName(renameRequest.newName)) {
            logger.debug("${cls.currentName}: Renamed to ${renameRequest.newName}")
            return true
        }
        return false
    }


    override fun renameField(renameRequest: InternalRenameRequest, field: IJavaField, cls: IDexClass): Boolean {
        val dexField = cls.getField(false, field.originalName, field.type.signature)
        if (dexField == null) {
            logger.warning("Error: Failed to get dex field. ${cls.currentName}::${field.originalName} -> ${renameRequest.newName}")
            return false
        }
        if (renameField(renameRequest, dexField)) {
            logger.debug("${cls.currentName}: Field ${dexField.originalName} Renamed to ${renameRequest.newName}")
            return true
        }
        return false
    }

    override fun renameField(renameRequest: InternalRenameRequest, field: IJavaField, unit: IDexUnit): Boolean {
        val dexField = unit.getField(field.signature)
        if (dexField == null) {
            logger.warning("Error: Failed to get dex field from unit. ::${field.originalName} -> ${renameRequest.newName}")
            return false
        }
        if (renameField(renameRequest, dexField)) {
            val cls = dexField.classType?.implementingClass
            if (cls == null) {
                logger.debug("Field ${dexField.originalSignature} Renamed to ${renameRequest.newName}")
            } else {
                logger.debug("${cls.currentName}: Field ${dexField.originalName} Renamed to ${renameRequest.newName}")
            }
            return true
        }
        return false
    }

    override fun renameField(renameRequest: InternalRenameRequest, field: IDexField): Boolean =
        field.setName(renameRequest.newName)

    override fun renameMethod(renameRequest: InternalRenameRequest, method: IDexMethod, cls: IDexClass): Boolean {
        if (method.setName(renameRequest.newName)) {
            logger.debug("${cls.currentName}: Renamed method ${method.originalName} to ${renameRequest.newName}")
            return true
        }
        return false
    }

    override fun renameIdentifier(
        renameRequest: InternalRenameRequest,
        identifier: IJavaIdentifier,
        unit: IDexUnit
    ): Boolean {
        val decompiler = decompilerCache.getOrPut(unit, unit::decompilerRef)
        if (decompiler.setIdentifierName(identifier, renameRequest.newName)) {
            logger.debug("Renamed identifier $identifier to ${renameRequest.newName}")
            return true
        }
        return false
    }
}