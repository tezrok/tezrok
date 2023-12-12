package io.tezrok.core

import io.tezrok.BaseTest
import io.tezrok.util.PathUtil
import io.tezrok.util.ResourceUtil
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

/**
 * Test for [TezrokBuilder]
 */
@Disabled("Manual test")
internal class TezrokBuilderTest : BaseTest() {
    private val fixedClock = getFixedClock()

    @Test
    fun testGenerateSimpleProject() {
        val projectPath = ResourceUtil.getResourceAsPath("/projects/tezrok-simple.json")
        val projectOutput = PathUtil.resolve("../../output/tezrok-simple")

        TezrokBuilder.from(projectPath)
            .setOutput(projectOutput)
            .setOutputFinalProject(true)
            .setAuthor("tezrokAdmin")
            .setClock(fixedClock)
            .generate()
    }

    @Test
    fun testGenerateTimelineProject() {
        val projectRoot = System.getProperty("timelineRoot") ?: error("timelineRoot is not set")
        val projectPath = PathUtil.resolve("$projectRoot/tezrok/tezrok-timeline.json")
        val projectOutput = PathUtil.resolve("$projectRoot/project")

        TezrokBuilder.from(projectPath)
            .setOutput(projectOutput)
            .setOutputFinalProject(true)
            .setGenerateTime(false)
            .setAuthor("timelineAdmin")
            .setFinalProjectPath(projectPath.parent)
            .setClock(getFixedClock("2023-08-17T20:57:04.00Z"))
            .generate()
    }
}
