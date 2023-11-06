package com.yoavst.jeb.utils.renaming

data class InternalRenameRequest(
        val type: RenameObjectType,
        val currentName: String,
        val newName: String,
        val reason: RenameReason,
        /**
         *  Do we try to rename just to provide the user extra info about the class,
         * or we actually try to get the real name of the class **/
        val informationalRename: Boolean = false
) {
    companion object {
        fun ofClass(currentName: String, newName: String, reason: RenameReason, informationalRename: Boolean = false) =
                InternalRenameRequest(RenameObjectType.Class, currentName, newName, reason, informationalRename)

        fun ofMethod(currentName: String, newName: String, reason: RenameReason, informationalRename: Boolean = false) =
                InternalRenameRequest(RenameObjectType.Method, currentName, newName, reason, informationalRename)

        fun ofField(currentName: String, newName: String, reason: RenameReason, informationalRename: Boolean = false) =
                InternalRenameRequest(RenameObjectType.Field, currentName, newName, reason, informationalRename)

        fun ofIdentifier(currentName: String, newName: String, reason: RenameReason, informationalRename: Boolean = false) =
                InternalRenameRequest(RenameObjectType.Identifier, currentName, newName, reason, informationalRename)
    }
}
