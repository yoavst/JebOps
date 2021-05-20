package com.yoavst.jeb.utils

import org.apache.commons.text.StringEscapeUtils

fun String?.orIfBlank(other: String) = if (isNullOrBlank()) other else this
inline fun <T, U> Sequence<T>.mapToPair(crossinline f: (T) -> U): Sequence<Pair<T, U>> = map { it to f(it) }
inline fun <T, U> Sequence<T>.mapToPairNotNull(crossinline f: (T) -> U?): Sequence<Pair<T, U>> = mapNotNull {
    val res = f(it)
    if (res == null) null else it to res
}

fun String.unescape(): String = StringEscapeUtils.unescapeJava(this)