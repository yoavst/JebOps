package com.yoavst.jeb.utils

fun String?.orIfBlank(other: String) = if (isNullOrBlank()) other else this
inline fun <T, U> Sequence<T>.mapToPair(crossinline f: (T) -> U): Sequence<Pair<T, U>> = map { it to f(it) }