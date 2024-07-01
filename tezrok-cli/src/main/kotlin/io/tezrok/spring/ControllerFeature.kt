package io.tezrok.spring

import io.tezrok.api.GeneratorContext
import io.tezrok.api.ProcessModelPhase
import io.tezrok.api.TezrokFeature
import io.tezrok.api.input.*
import io.tezrok.api.java.JavaDirectoryNode
import io.tezrok.api.maven.ProjectNode
import io.tezrok.util.getSetterName
import io.tezrok.util.lowerFirst
import org.slf4j.LoggerFactory

/**
 * Creates controller class for each entity.
 */
internal class ControllerFeature : TezrokFeature {
    override fun apply(project: ProjectNode, context: GeneratorContext): Boolean {
        val module = project.getSingleModule()
        val applicationPackageRoot = module.source.main.java.applicationPackageRoot
        val pom = module.pom
        pom.addDependency("org.springframework.boot:spring-boot-starter-web:${'$'}{spring-boot.version}")
        pom.addDependency("org.apache.httpcomponents.client5:httpclient5:5.3.1")

        if (applicationPackageRoot != null) {
            val projectElem = context.getProject()
            val restDir = applicationPackageRoot.getOrAddJavaDirectory("web").getOrAddJavaDirectory("rest")
            val schema = context.getProject().modules.find { it.name == module.getName() }?.schema

            schema?.entities?.forEach { entity ->
                addControllerClass(entity, restDir, projectElem.packagePath, context)
            }

            addRestHandlerExceptionResolver(
                applicationPackageRoot.getOrAddJavaDirectory("error"),
                projectElem.packagePath,
                context
            )
            val configDir = applicationPackageRoot.getOrAddJavaDirectory("config")
            context.addFile(configDir, "/templates/spring/WebMvcConfig.java.vm")
            context.addFile(configDir, "/templates/spring/AlternativePathResourceResolver.java.vm")
        } else {
            log.warn("Application package root is not set, module: {}", module.getName())
        }

        return true
    }

    private fun addRestHandlerExceptionResolver(
        restDir: JavaDirectoryNode,
        packagePath: String,
        context: GeneratorContext
    ) {
        val fileName = "RestHandlerExceptionResolver.java"
        if (!restDir.hasClass(fileName)) {
            val controllerFile = restDir.addJavaFile(fileName)
            context.writeTemplate(
                controllerFile,
                "/templates/spring/RestHandlerExceptionResolver.java.vm",
                mapOf("package" to packagePath)
            )
        }
    }

    private fun addControllerClass(
        entity: EntityElem,
        webDir: JavaDirectoryNode,
        packagePath: String,
        context: GeneratorContext
    ) {
        if (entity.syntheticTo?.isNotBlank() == true) {
            // skip synthetic entities
            return
        }
        if (entity.skipController == true) {
            // skip entities with skipController flag
            return
        }

        val name = entity.name
        val fileName = "${name}ApiController.java"

        if (!webDir.hasClass(fileName)) {
            val controllerFile = webDir.addJavaFile(fileName)
            val values = mutableMapOf<String, Any>(
                "package" to packagePath,
                "name" to name,
                "lname" to name.lowerFirst(),
                "primarySetter" to entity.getPrimaryField().getSetterName()
            )
            entity.methods.filter { it.isApi() && it.skipGenerate == true }.forEach { method ->
                values[getBaseName(method.name, entity.name)] = true
            }
            context.writeTemplate(controllerFile, "/templates/spring/EntityApiController.java.vm", values)
            val controllerClass = controllerFile.getRootClass()
            entity.methods.filter { it.isApi() && it.skipGenerate != true }.forEach { method ->
                // TODO: create method in controller
            }
        } else {
            log.warn("File already exists: {}", fileName)
        }
    }

    private fun getBaseName(name: String, entityName: String): String {
        return when (name) {
            "getFull${entityName}ById" -> "getFullEntityById"
            "createFull${entityName}" -> "createFullEntity"
            "updateFull${entityName}" -> "updateFullEntity"
            "search${entityName}sByTerm" -> "searchEntitiesByTerm"
            "importFull${entityName}" -> "importFullEntity"
            else -> name
        }
    }

    override fun processModel(project: ProjectElem, phase: ProcessModelPhase): ProjectElem {
        if (phase != ProcessModelPhase.Process) {
            // synthetic fields should be added before, so we need Process phase
            return project
        }

        return project.copy(modules = project.modules.map { processModule(it) })
    }

    private fun processModule(module: ModuleElem): ModuleElem {
        return module.copy(schema = module.schema?.copy(entities = module.schema.entities?.map { processEntity(it) }))
    }

    /**
     * Add standard methods by entity relations.
     */
    private fun processEntity(entity: EntityElem): EntityElem {
        val entityName = entity.name
        val entityDto = "${entityName}Dto"
        val entityDtoFull = "${entityName}FullDto"
        val methodsMap = entity.methods.associateBy { it.name }.toMutableMap()
        val methods = mutableSetOf<MethodElem>()
        methods.add(
            createApiMethod(
                "findAll", "Finds all {@link $entityDto}s.",
                entity.stdMethodProps,
                methodsMap
            )
        )
        methods.add(
            createApiMethod(
                "getById",
                "Gets {@link $entityDto} by id.",
                entity.stdMethodProps,
                methodsMap
            )
        )
        methods.add(
            createApiMethod(
                "getFull${entityName}ById",
                "Gets {@link $entityDtoFull} by id.",
                entity.stdMethodProps,
                methodsMap
            )
        )
        methods.add(
            createApiMethod(
                "createFull${entityName}",
                "Creates {@link $entityDtoFull}.",
                entity.stdMethodProps,
                methodsMap
            )
        )
        methods.add(
            createApiMethod(
                "updateFull${entityName}",
                "Updates existing {@link $entityDtoFull} by id.",
                entity.stdMethodProps,
                methodsMap
            )
        )
        methods.add(
            createApiMethod(
                "importFull${entityName}",
                "Imports {@link $entityDtoFull}.",
                entity.stdMethodProps,
                methodsMap
            )
        )
        methods.add(
            createApiMethod(
                "search${entityName}sByTerm",
                "Search {@link $entityDto} by term (by all string fields).",
                entity.stdMethodProps,
                methodsMap
            )
        )
        methods.addAll(methodsMap.values)
        return entity.copy(methods = methods)
    }

    private fun createApiMethod(
        name: String,
        description: String,
        commonProps: MethodProps?,
        methodsMap: MutableMap<String, MethodElem>
    ): MethodElem {
        val inheritedMethod = methodsMap.remove(name)
        return MethodElem(
            name = name,
            description = inheritedMethod?.description ?: description,
            api = commonProps?.api ?: inheritedMethod?.api ?: true,
            roles = commonProps?.roles ?: inheritedMethod?.roles,
            permissions = commonProps?.permissions ?: inheritedMethod?.permissions,
            skipGenerate = true
        )
    }

    private companion object {
        val log = LoggerFactory.getLogger(ControllerFeature::class.java)!!
    }
}
