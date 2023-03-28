package io.tezrok.core

import io.tezrok.api.GeneratorContext
import java.time.Clock

internal class CoreGeneratorContext(private val clock: Clock = Clock.systemDefaultZone()) : GeneratorContext {
    override fun getAuthor(): String = "TezrokUser"

    override fun getClock(): Clock = clock
}
