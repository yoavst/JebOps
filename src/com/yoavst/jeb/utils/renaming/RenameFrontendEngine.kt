package com.yoavst.jeb.utils.renaming

interface RenameFrontendEngine {
    /**
     * Process the renaming request and return a rename request with the final name to be applied.
     * Returns null if the request is denied
     */
    fun applyRules(renameRequest: InternalRenameRequest): InternalRenameRequest?
}