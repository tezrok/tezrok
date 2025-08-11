package io.tezrok.cli

import io.tezrok.core.TezrokBuilder
import picocli.CommandLine
import java.nio.file.Path
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.Callable
import kotlin.io.path.exists

/**
 * Tezrok CLI Application
 */
@CommandLine.Command(
    name = "tezrok",
    version = ["1.1-SNAPSHOT"],
    description = ["Tezrok CLI - a tool for generating java-backend projects"],
    mixinStandardHelpOptions = true
)
class TezrokCliApp : Callable<Int> {
    @CommandLine.Parameters(index = "0", paramLabel = "PROJECT-FILE", description = ["input project file"])
    private var projectPath: Path? = null

    @CommandLine.Option(
        names = ["-o", "--output"],
        description = ["output directory for generated project files"],
        defaultValue = "output"
    )
    private var projectOutput: Path? = null

    @CommandLine.Option(
        names = ["--fixed-time"],
        description = ["use fixed time (example: 2025-04-12T14:32:54.00Z)"],
    )
    private var fixedTime: String? = null

    override fun call(): Int {
        val projectPath = projectPath ?: error("Project file path is required")

        if (!projectPath.exists()) {
            System.err.println("Project file does not exist: $projectPath")
            return 2
        }

        val projectOutput = projectOutput ?: projectPath.parent.resolve("output")

        val builder = TezrokBuilder.from(projectPath)
            .setOutput(projectOutput)
            .setOutputFinalProject(true)
            .setGenerateTime(false)
            .setAuthorLogin("timelineAdmin")
            .setFinalProjectPath(projectPath.parent)

        val fixedTime = fixedTime
        if (fixedTime != null) {
            builder.setClock(getFixedClock(fixedTime))
        }

        builder.generate()

        return 0
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val exitCode = CommandLine(TezrokCliApp()).execute(*args)
            System.exit(exitCode)
        }

        private fun getFixedClock(timeStr: String): Clock =
            Clock.fixed(Instant.parse(timeStr), ZoneId.systemDefault())
    }
}
