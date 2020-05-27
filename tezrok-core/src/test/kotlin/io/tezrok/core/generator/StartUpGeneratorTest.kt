package io.tezrok.core.generator

import io.tezrok.api.model.node.FeatureNode
import io.tezrok.api.model.node.ModuleNode
import io.tezrok.api.model.node.ProjectNode
import io.tezrok.core.factory.MainFactory
import org.junit.jupiter.api.Test
import java.io.File

class StartUpGeneratorTest {
    @Test
    fun testMainAppGenerator() {
        val project = ProjectNode("Hello World", "Simple Hello World App")
        project.add(ModuleNode("Hello World", "com.company", "1.0")
                .use(FeatureNode("HelloWorld")))

        val targetDir = File("output")
        val mainFactory = MainFactory.create(project, targetDir)
        val generator = mainFactory.getInstance(StartUpGenerator::class.java)

        generator.generate()
    }
}
