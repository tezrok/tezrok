package io.tezrok.util

import java.util.stream.Stream

/**
 * Find first element in stream that matches predicate or return null
 */
fun <T> Stream<T>.find(predicate: (T) -> Boolean): T? = filter(predicate).findFirst().orElse(null)
