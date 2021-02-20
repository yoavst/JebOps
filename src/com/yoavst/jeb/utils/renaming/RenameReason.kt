package com.yoavst.jeb.utils.renaming

/**
 * Represents a reason for a renaming request.
 * The higher the ordinal, the bigger priority it has.
 */
enum class RenameReason(val prefix: String) : Comparable<RenameReason> {
    Type("T"),
    Log("L"),
    MethodStringArgument("A"),
    ToString("TS"),
    EnumName("E"),
    /** The user renamed the class. Should not be used as a reason */
    User("");

    companion object {
        const val MAX_PREFIX_LENGTH = 2
        /** Allocate it once for performance reason */
        private val VALUES = values()

        fun fromPrefix(prefix: String) = VALUES.firstOrNull { it.prefix == prefix }
    }
}