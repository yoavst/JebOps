package com.yoavst.jeb.utils

import org.apache.commons.text.StringEscapeUtils
import java.util.stream.Stream


fun String?.orIfBlank(other: String) = if (isNullOrBlank()) other else this

inline fun <T, U> Sequence<T>.mapToPair(crossinline f: (T) -> U): Sequence<Pair<T, U>> = map {
    it to f(it)
}

inline fun <T, U> Stream<T>.mapToPair(crossinline f: (T) -> U): Stream<Pair<T, U>> = map {
    it to f(it)
}

inline fun <T, U> Stream<out T>.mapNotNull(crossinline f: (T) -> U): Stream<U> = map {
    val result = f(it)
    result
}


inline fun <T, U> Sequence<T>.mapToPairNotNull(crossinline f: (T) -> U?): Sequence<Pair<T, U>> = mapNotNull {
    val result = f(it)
    if (result == null) {
        null
    } else {
        it to result
    }
}


inline fun <T, U> Stream<out T>.mapToPairNotNull(crossinline f: (T) -> U?): Stream<Pair<T, U>> = map {
    val result = f(it)
    if (result == null) {
        null
    } else {
        it to result
    }
}


fun String.unescape(): String = StringEscapeUtils.unescapeJava(this)