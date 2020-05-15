package io.tezrok.parser

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import io.tezrok.model.Project

class ModelParser {
    private val objectMapper = ObjectMapper().apply {
        setSerializationInclusion(JsonInclude.Include.NON_NULL)
    }

    fun format(project: Project): String {
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(project)
    }
}
