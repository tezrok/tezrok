package io.tezrok.core.input

import com.fasterxml.jackson.annotation.JsonProperty
import io.tezrok.api.Schema

/**
 * Represents a model of a project loaded from a tezrok.json file
 */
class ProjectElem {
    var name: String = ""
    var version: String = ""
    var description: String = ""

    @JsonProperty("package")
    var packagePath: String = ""
    var author: String = ""
    var modules: List<ModuleElem> = emptyList()

    override fun toString(): String {
        return "ProjectElem(name='$name')"
    }
}

class ModuleElem {
    var name: String = ""
    var description: String = ""
    var type: String = "" // TODO: enum
    var importSchema: String = ""
    // TODO: remove schema property and use entities instead
    var schema: Schema? = null
    // TODO: convert schema to entities
    var entities: List<EntityElem> = emptyList()

    override fun toString(): String {
        return "ModuleElem(name='$name')"
    }
}

class EntityElem {
    init {
        TODO()
    }
}
