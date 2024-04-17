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
            .setAuthorLogin("tezrokAdmin")
            .setClock(fixedClock)
            .generate()
    }

    @Test
    fun testGenerateTimelineProject() {
        val projectRoot = System.getProperty("timelineRoot") ?: error("timelineRoot is not set")
        val projectPath = PathUtil.resolve("$projectRoot/tezrok/tezrok-timeline.json")
        val projectOutput = PathUtil.resolve("$projectRoot/timeline-app")

        TezrokBuilder.from(projectPath)
            .setOutput(projectOutput)
            .setOutputFinalProject(true)
            .setGenerateTime(false)
            .setAuthorLogin("timelineAdmin")
            .setFinalProjectPath(projectPath.parent)
            .setClock(getFixedClock("2023-08-17T20:57:04.00Z"))
            .generate()
    }

    @Test
    fun testGenerateTezrokQAProject() {
        val projectRoot = System.getProperty("tezrokQARoot") ?: error("tezrokQARoot is not set")
        val projectPath = PathUtil.resolve("$projectRoot/tezrok-qa.json")
        val projectOutput = PathUtil.resolve("$projectRoot/")

        TezrokBuilder.from(projectPath)
            .setOutput(projectOutput)
            .setOutputFinalProject(true)
            .setGenerateTime(false)
            .setAuthorLogin("tezrokQa")
            .setFinalProjectPath(projectPath.parent)
            .setClock(getFixedClock("2024-02-04T11:20:42.00Z"))
            .generate()
    }
}
