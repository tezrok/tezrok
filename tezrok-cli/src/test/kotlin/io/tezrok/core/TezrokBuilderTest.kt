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
    fun testGenerateProject() {
        val projectPath = ResourceUtil.getResourceAsPath("/projects/tezrok-simple.json")
        val projectOutput = PathUtil.resolve("../../output/tezrok-simple")

        TezrokBuilder.from(projectPath)
                .setOutput(projectOutput)
                .setOutputFinalProject(true)
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
            .setFinalProjectPath(projectPath.parent)
            .setClock(fixedClock)
            .generate()
    }
}
