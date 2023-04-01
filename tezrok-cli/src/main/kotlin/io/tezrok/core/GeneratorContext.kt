package io.tezrok.core

import io.tezrok.api.GeneratorContext
import io.tezrok.core.input.ProjectElem
import java.time.Clock

internal class CoreGeneratorContext(
    private val project: ProjectElem,
    private val clock: Clock = Clock.systemDefaultZone()
) : GeneratorContext {
    override fun getAuthor(): String = "TezrokUser"

    override fun getClock(): Clock = clock

    override fun getProject(): ProjectElem = project
}
