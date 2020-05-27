package io.tezrok.core.generator

import io.tezrok.api.Generator
import io.tezrok.api.GeneratorContext
import org.slf4j.LoggerFactory

class HelloWorldGenerator(context: GeneratorContext) : Generator {
    private val log = LoggerFactory.getLogger(HelloWorldGenerator::class.java)

    override fun generate() {
        log.warn("HelloWorldGenerator not implemented")
    }
}
