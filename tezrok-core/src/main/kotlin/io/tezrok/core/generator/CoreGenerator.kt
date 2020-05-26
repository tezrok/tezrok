package io.tezrok.core.generator

import io.tezrok.api.Generator
import io.tezrok.api.GeneratorContext
import org.slf4j.LoggerFactory

/**
 * Main application class generator
 */
class CoreGenerator(private val context: GeneratorContext) : Generator {
    private val log = LoggerFactory.getLogger(CoreGenerator::class.java)

    override fun generate() {
        log.info("TODO: Generating main app...")
    }
}
