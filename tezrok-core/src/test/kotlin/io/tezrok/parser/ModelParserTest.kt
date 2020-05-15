package io.tezrok.parser

import io.tezrok.model.Entity
import io.tezrok.model.Field
import io.tezrok.model.Module
import io.tezrok.model.Project
import org.junit.jupiter.api.Test

class ModelParserTest {

    @Test
    fun testFormat() {
        val parser = ModelParser()

        val fields = listOf(Field("id", "Int", primary = true),
                Field("title", "String", max = 200, min = 1))
        val entities = listOf(Entity("Post", "Post", fields = fields))
        val modules = listOf(Module("App", "Main application", entities = entities))
        val project = Project(name = "Personal blog", description = "My personal blog",
                modules = modules)
        val json = parser.format(project)

        println(json)

        println("------------------------------------")

        val entity = project.modules!!.first().entities!!.first()

        println(entity["name"])
        println(entity["fields"])
    }
}
