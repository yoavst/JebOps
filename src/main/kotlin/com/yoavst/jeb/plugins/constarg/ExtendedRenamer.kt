package com.yoavst.jeb.plugins.constarg

class ExtendedRenamer(val constArgIndex: Int, func: (String) -> RenameResult, val renamedArgumentIndex: Int? = null) :
        (String) -> RenameResult by func