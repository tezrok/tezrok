package io.tezrok.core.generator

import io.tezrok.api.ExecuteContext
import io.tezrok.api.Generator
import org.slf4j.LoggerFactory

/**
 * Main application class generator
 */
class CoreGenerator : Generator {
    private val log = LoggerFactory.getLogger(CoreGenerator::class.java)

    override fun execute(context: ExecuteContext) {
        log.info("TODO: Generating main app...")
    }
}
