package com.yoavst.jeb.utils.renaming

object RenameFrontendEngineImpl : RenameFrontendEngine {
    private val PreservedShortNames = setOf("run", "act", "get", "set", "let")

    override fun applyRules(renameRequest: InternalRenameRequest): InternalRenameRequest? {
        val (type, currentName, newName, reason, informationalRename) = renameRequest
        val newNameSanitized = newName.sanitize()
        if (currentName.isBlank()) {
            return renameRequest
        } else if (!generalShouldRename(currentName, newNameSanitized, reason)) {
            return null
        } else if (!informationalRename && currentName.length > 3) {
            when (type) {
                RenameObjectType.Class -> {
                    // check for CamelCase
                    if (currentName[0].isUpperCase() && currentName.any(Char::isLowerCase))
                        return null
                }
                RenameObjectType.Method -> {
                    // is it getter or setter or constructor
                    if (currentName.startsWith("get") || currentName.startsWith("set") ||
                        currentName == "<init>" || currentName == "<clinit>"
                    )
                        return null
                    // is it a camelCase?
                    if (currentName[0].isLowerCase() && currentName.any(Char::isUpperCase))
                        return null
                }
                RenameObjectType.Field -> {
                    // check for mVariable or sVariable
                    if ((currentName[0] == 'm' || currentName[0] == 's') && currentName[1].isUpperCase())
                        return null
                }
                RenameObjectType.Identifier -> {
                    if (currentName[0].isLowerCase() && currentName.any(Char::isUpperCase))
                        return null
                }
            }
        }

        // check if currentName is a valid java name
        val currentModifiedName = if (currentName.isValidJavaIdentifier()) currentName else "$$currentName"

        if (informationalRename) {
            // we want to keep the current name
            return InternalRenameRequest(
                type,
                currentName,
                "${currentModifiedName}__${reason.prefix}_${newNameSanitized}",
                reason,
                informationalRename
            )
        } else if (type == RenameObjectType.Identifier) {
            // We want to erase the previous name since it isn't a real name
            return InternalRenameRequest(
                type,
                currentName,
                "__${reason.prefix}_${newNameSanitized}",
                reason,
                informationalRename
            )
        } else {
            return InternalRenameRequest(
                type,
                currentName,
                "${currentModifiedName.extractOriginalName()}__${reason.prefix}_${newNameSanitized}",
                reason,
                informationalRename
            )
        }

    }

    /** Common rules for whether should rename from `name` to `newName` */
    private fun generalShouldRename(name: String, newName: String, reason: RenameReason): Boolean {
        if (newName.isBlank())
            return false

        if (name.contains(newName, ignoreCase = true))
            return false

        if (name.length <= 3) {
            return name !in PreservedShortNames
        }

        if (newName.contains(name, ignoreCase = true))
            return false

        if ('$' in name)
            return false

        val oldReason = getModifiedInfo(name)?.second ?: return true
        return reason >= oldReason
    }

    /**
     * Tries to extract the rename reason encoded in the given `name`.
     * It assumes normal name does not have an underscore in its name.
     * This is probably justified since this is against Java naming convention.
     * Some generated classes may contain __ in name, but that mean
     * they have the original name, so it's ok.
     **/
    override fun getModifiedInfo(name: String): Pair<String, RenameReason?>? {
        var nameParts = name.split("__", limit = 2)
        if (nameParts.size == 1) {
            nameParts = name.split("_", limit = 2)
            if (nameParts.size == 1) {
                return null
            }
            // assume there is no reason for _ to be in name we want to rename.
            // The only reason it actually make sense is if we try to rename constant.
            // And in this case, it's ok to fail since we probably want to retain the original name.
            return nameParts[1] to null
        }
        val generatedNameParts = nameParts[1].split("_", limit = 2)
        if (generatedNameParts.size == 1) {
            // The user was too lazy to remove the extra underscore, ok...
            return generatedNameParts[0] to null
        }
        val prefix = generatedNameParts[0]
        if (RenameReason.MAX_PREFIX_LENGTH < prefix.length) return null
        return generatedNameParts[1] to RenameReason.fromPrefix(prefix)
    }

    private fun String.extractOriginalName(): String {
        var nameParts = split("__", limit = 2)
        if (nameParts.size == 1) {
            nameParts = split("_", limit = 2)
        }
        return nameParts[0]
    }

    private fun String.isValidJavaIdentifier(): Boolean {
        if (isEmpty()) {
            return false
        }
        if (!this[0].isJavaIdentifierStart()) {
            return false
        }
        for (i in 1 until length) {
            if (!this[i].isJavaIdentifierPart()) {
                return false
            }
        }
        return true
    }

    private fun String.sanitize(): String = trim().map { if (it.isJavaIdentifierPart()) it else "_" }.joinToString("")
}