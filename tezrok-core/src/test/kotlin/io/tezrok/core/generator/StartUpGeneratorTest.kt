package io.tezrok.core.generator

import io.tezrok.api.model.node.*
import io.tezrok.core.factory.MainFactory
import org.junit.jupiter.api.Test
import java.io.File

class StartUpGeneratorTest {
    @Test
    fun testMainAppGenerator() {
        val project = ProjectNode("Hello World", "Simple Hello World App")
        val module = ModuleNode("Hello World", "com.company", "1.0", project).apply {
            use(FeatureNode("HelloWorld", this))
            use(FeatureNode("SpringJPA", this))
            add(EntityNode("Post", this, "Blog post").apply {
                add(FieldNode("id", "Long", this, primary = true))
                add(FieldNode("title", "String", this, max = 100))
                add(FieldNode("text", "String", this, max = 8000))
            })
        }

        project.add(module)

        val targetDir = File("output")
        val mainFactory = MainFactory.create(project, targetDir)
        val generator = mainFactory.getInstance(StartUpGenerator::class.java)

        generator.execute()
    }
}
