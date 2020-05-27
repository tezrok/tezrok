package io.tezrok.core.factory

import io.tezrok.api.Generator
import io.tezrok.api.visitor.MavenVisitor

/**
 * Creates instances of all classes
 */
interface Factory {
    fun <T> getInstance(clazz: Class<T>): T

    fun getGenerator(className: String): Generator

    fun getMavenVisitors(): List<MavenVisitor>
}
