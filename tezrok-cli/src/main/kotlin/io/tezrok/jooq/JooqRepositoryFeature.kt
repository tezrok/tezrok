package io.tezrok.jooq

import io.tezrok.api.GeneratorContext
import io.tezrok.api.TezrokFeature
import io.tezrok.api.maven.ProjectNode
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository

/**
 * Creates repository class for each entity.
 */
internal class JooqRepositoryFeature : TezrokFeature {
    override fun apply(project: ProjectNode, context: GeneratorContext): Boolean {
        val module = project.getSingleModule()
        val javaRoot = module.source.main.java
        val applicationPackageRoot = javaRoot.applicationPackageRoot
        val projectElem = context.getProject()

        if (applicationPackageRoot != null) {
            val repositoryDir = applicationPackageRoot.getOrAddJavaDirectory("repository")
            val jooqRepoFile = repositoryDir.getOrAddFile("JooqRepository.java")

            if (jooqRepoFile.isEmpty()) {
                context.writeTemplate(jooqRepoFile, "/templates/liquibase/JooqRepository.java.vm") { velContext ->
                    velContext.put("package", context.getProject().packagePath + ".repository")
                }
            } else {
                log.warn("File ${jooqRepoFile.getName()} already exists")
            }

            val schemaModule = context.getProject().modules.find { it.name == module.getName() }
                    ?: throw IllegalStateException("Module ${module.getName()} not found")

            val jooqPackageRoot = projectElem.packagePath + ".jooq"

            schemaModule.schema?.definitions?.keys?.forEach { name ->
                val className = "${name}Repository"

                if (!repositoryDir.hasFile("$className.java")) {
                    val repoClass = repositoryDir.addClass(className)
                    repoClass.extendClass("JooqRepository<${name}Record, Long, $name>")
                    repoClass.addImport("$jooqPackageRoot.Tables")
                    repoClass.addImport("$jooqPackageRoot.tables.pojos.$name")
                    repoClass.addImport("$jooqPackageRoot.tables.records.${name}Record")
                    repoClass.addImport(DSLContext::class.java)
                    repoClass.addAnnotation(Repository::class.java)
                    repoClass.addConstructor()
                            .addParameter(DSLContext::class.java, "dsl")
                            .addAnnotation(Autowired::class.java)
                            .addCallSuperExpression()
                            .addNameArgument("dsl")
                            .addNameArgument("Tables.${name.uppercase()}")
                            .addNameArgument("Tables.${name.uppercase()}.ID")
                            .addNameArgument("$name.class")
                }
            }
        } else {
            log.warn("Application package root is not set")
        }

        return true
    }

    private companion object {
        val log = LoggerFactory.getLogger(JooqRepositoryFeature::class.java)!!
    }
}
