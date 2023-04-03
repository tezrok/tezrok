package io.tezrok.core.output

import io.tezrok.core.CoreGeneratorContext
import io.tezrok.core.feature.FeatureManager
import io.tezrok.core.input.ProjectElemRepository
import io.tezrok.util.PathUtil
import io.tezrok.util.ResourceUtil
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

/**
 * Test for [ProjectOutputGenerator]
 */
@Disabled("Manual test")
internal class ProjectOutputGeneratorTest {
    private val projectElemRepository = ProjectElemRepository()
    private val generator = ProjectOutputGenerator()
    private val projectNodeFactory = ProjectNodeFactory(projectElemRepository)

    @Test
    fun testGenerateProject() {
        val projectPath = ResourceUtil.getResourceAsPath("/projects/tezrok-simple.json")
        val projectElem = projectElemRepository.load(projectPath)
        val project = projectNodeFactory.fromProject(projectElem)
        val featureManager = FeatureManager()
        val context = CoreGeneratorContext(projectElem)

        featureManager.applyAll(project, context)

        val projectOutput = PathUtil.resolve("output/tezrok-simple")
        generator.generate(project, projectOutput)
    }
}
