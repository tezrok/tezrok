package io.tezrok.db

import io.tezrok.api.GeneratorContext
import io.tezrok.api.TezrokFeature
import io.tezrok.api.TezrokProperties
import io.tezrok.api.maven.ProjectNode

/**
 * Adds a datasource properties to the project.
 */
internal class DataSourceFeature : TezrokFeature {
    override fun apply(project: ProjectNode, context: GeneratorContext): Boolean {
        val module = project.getSingleModule()
        val properties = module.properties
        val moduleName = module.getName()
        val dbName = "${moduleName}Db"

        properties.setPropertyIf("datasource.db-name", dbName)
        properties.setPropertyIf("datasource.url", "jdbc:postgresql://localhost:5432/$dbName")
        properties.setPropertyIf("datasource.username", context.getAuthorLogin())
        properties.setPropertyIf("datasource.password", "${context.getAuthorLogin()}Pwd")
        properties.setPropertyIf("datasource.driver-class-name", "org.postgresql.Driver")
        properties.setPropertyIf("datasource.dbms", "PostgreSQL")

        return true
    }

    private fun TezrokProperties.setPropertyIf(key: String, value: String?) {
        if (!hasProperty(key)) {
            setProperty(key, value)
        }
    }
}
