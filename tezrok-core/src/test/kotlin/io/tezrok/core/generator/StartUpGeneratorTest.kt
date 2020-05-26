package io.tezrok.core.generator

import io.tezrok.api.model.node.FeatureNode
import io.tezrok.api.model.node.ModuleNode
import io.tezrok.api.model.node.ProjectNode
import io.tezrok.core.factory.MainFactory
import org.junit.jupiter.api.Test

class StartUpGeneratorTest {
    @Test
    fun testMainAppGenerator() {
        val project = ProjectNode("Hello World", "Simple Hello World App")
        project.add(ModuleNode("Hello World", "")
                .use(FeatureNode("HelloWorld")))

        val mainFactory = MainFactory.create(project)
        val generator = mainFactory.getInstance(StartUpGenerator::class.java)

        generator.generate()
    }
}
