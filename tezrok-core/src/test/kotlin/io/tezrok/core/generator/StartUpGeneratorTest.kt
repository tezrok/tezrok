package io.tezrok.core.generator

import io.tezrok.api.model.node.*
import io.tezrok.core.factory.MainFactory
import org.junit.jupiter.api.Test
import java.io.File

class StartUpGeneratorTest {
    @Test
    fun testMainAppGenerator() {
        val project = ProjectNode("Hello World", "Simple Hello World App")
        val module = ModuleNode("Hello World", "com.company", "1.0")
                .use(FeatureNode("HelloWorld"))
                .add(EntityNode("Post", "Blog post")
                        .add(FieldNode("id", "Long", primary = true))
                        .add(FieldNode("title", "String", max = 100))
                        .add(FieldNode("text", "String", max = 8000)))


        project.add(module)

        val targetDir = File("output")
        val mainFactory = MainFactory.create(project, targetDir)
        val generator = mainFactory.getInstance(StartUpGenerator::class.java)

        generator.execute()
    }
}
