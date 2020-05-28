package io.tezrok.core.generator

import io.tezrok.api.ExecuteContext
import io.tezrok.api.Generator
import org.slf4j.LoggerFactory

class HelloWorldGenerator : Generator {
    private val log = LoggerFactory.getLogger(HelloWorldGenerator::class.java)

    override fun execute(context: ExecuteContext) {
        log.warn("HelloWorldGenerator not implemented")
    }
}
