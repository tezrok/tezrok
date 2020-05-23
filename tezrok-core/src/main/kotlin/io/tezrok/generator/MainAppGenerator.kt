package io.tezrok.generator

import io.tezrok.api.Generator
import org.slf4j.LoggerFactory

/**
 * Main application class generator
 */
class MainAppGenerator : Generator {
    private val log = LoggerFactory.getLogger(MainAppGenerator::class.java)

    override fun generate() {
        log.info("TODO: Generating main app...")
    }
}
