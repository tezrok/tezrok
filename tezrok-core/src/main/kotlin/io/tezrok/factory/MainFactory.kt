package io.tezrok.factory

import io.tezrok.error.TezrokException
import io.tezrok.generator.MainAppGenerator

/**
 * Implementation of Factory
 */
class MainFactory : Factory {
    override fun <T> create(clazz: Class<T>): T {
        val obj = if (clazz == MainAppGenerator::class.java) {
            MainAppGenerator()
        } else {
            throw TezrokException("Unsupported type: $clazz")
        }

        return obj as T
    }
}
