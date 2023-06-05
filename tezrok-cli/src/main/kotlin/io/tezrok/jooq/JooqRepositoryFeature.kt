package io.tezrok.jooq

import io.tezrok.api.GeneratorContext
import io.tezrok.api.TezrokFeature
import io.tezrok.api.java.JavaDirectoryNode
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
            val dtoDir = applicationPackageRoot.getOrAddJavaDirectory("dto")
            val jooqRepoFile = repositoryDir.getOrAddFile("JooqRepository.java")

            dtoDir.getOrAddClass("WithId")
                    .setInterface(true)
                    .setTypeParameters("ID")
                    .getOrAddMethod("getId")
                    .removeBody()
                    .setReturnType("ID")

            if (jooqRepoFile.isEmpty()) {
                context.writeTemplate(jooqRepoFile, "/templates/jooq/JooqRepository.java.vm") { velContext ->
                    velContext.put("package", context.getProject().packagePath + ".repository")
                    velContext.put("packageDto", context.getProject().packagePath + ".dto")
                }
            } else {
                log.warn("File ${jooqRepoFile.getName()} already exists")
            }

            val schemaModule = context.getProject().modules.find { it.name == module.getName() }
                    ?: throw IllegalStateException("Module ${module.getName()} not found")

            schemaModule.schema?.definitions?.keys?.forEach { name ->
                addDtoClass(dtoDir, name, projectElem.packagePath)
                addRepositoryClass(repositoryDir, name, projectElem.packagePath)
            }
        } else {
            log.warn("Application package root is not set")
        }

        return true
    }

    private fun addDtoClass(dtoDir: JavaDirectoryNode, name: String, rootPackage: String) {
        val jooqPackageRoot = "${rootPackage}.jooq"
        val className = "${name}Dto"
        if (!dtoDir.hasFile("$className.java")) {
            val repoClass = dtoDir.addClass(className)
            repoClass.extendClass("$jooqPackageRoot.tables.pojos.$name")
            repoClass.implementInterface("WithId<Long>")
        }
    }

    private fun addRepositoryClass(repositoryDir: JavaDirectoryNode, name: String, rootPackage: String) {
        val jooqPackageRoot = "${rootPackage}.jooq"
        val className = "${name}Repository"

        if (!repositoryDir.hasFile("$className.java")) {
            val repoClass = repositoryDir.addClass(className)
            repoClass.extendClass("JooqRepository<${name}Record, Long, ${name}Dto>")
            repoClass.addImport("$jooqPackageRoot.Tables")
            repoClass.addImport("$jooqPackageRoot.tables.records.${name}Record")
            repoClass.addImport("$rootPackage.dto.${name}Dto")

            repoClass.addImport(DSLContext::class.java)
            repoClass.addAnnotation(Repository::class.java)
            repoClass.addConstructor()
                    .addParameter(DSLContext::class.java, "dsl")
                    .addAnnotation(Autowired::class.java)
                    .addCallSuperExpression()
                    .addNameArgument("dsl")
                    .addNameArgument("Tables.${name.uppercase()}")
                    .addNameArgument("Tables.${name.uppercase()}.ID")
                    .addNameArgument("${name}Dto.class")
        }
    }

    private companion object {
        val log = LoggerFactory.getLogger(JooqRepositoryFeature::class.java)!!
    }
}
