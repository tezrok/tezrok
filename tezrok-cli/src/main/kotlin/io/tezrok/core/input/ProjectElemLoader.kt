package io.tezrok.core.input

import io.tezrok.schema.SchemaLoader
import io.tezrok.util.JsonUtil
import io.tezrok.util.toURL
import java.nio.file.Path

class ProjectElemLoader {
    fun load(projectPath: Path): ProjectElem {
        val project = JsonUtil.mapper.readValue(projectPath.toURL(), ProjectElem::class.java)

        for (module in project.modules) {
            if (module.importSchema.isNotBlank()) {
                val schemaPath = projectPath.parent.resolve(module.importSchema)
                val schemaLoader = SchemaLoader()
                val schema = schemaLoader.load(schemaPath)
                module.schema = schema
            }
        }

        return project
    }
}
