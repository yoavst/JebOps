package com.yoavst.jeb.utils.renaming

data class RenameRequest(
    val newName: String,
    val reason: RenameReason,
    /**
     *  Do we try to rename just to provide the user extra info about the class,
     * or we actually try to get the real name of the class **/
    val informationalRename: Boolean = false
)
