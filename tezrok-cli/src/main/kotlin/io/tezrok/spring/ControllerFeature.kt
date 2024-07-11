package io.tezrok.spring

import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.expr.BooleanLiteralExpr
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.expr.StringLiteralExpr
import io.tezrok.api.GeneratorContext
import io.tezrok.api.ProcessModelPhase
import io.tezrok.api.TezrokFeature
import io.tezrok.api.input.*
import io.tezrok.api.java.JavaClassNode
import io.tezrok.api.java.JavaDirectoryNode
import io.tezrok.api.java.JavaMethodNode
import io.tezrok.api.maven.ProjectNode
import io.tezrok.util.getSetterName
import io.tezrok.util.lowerFirst
import io.tezrok.util.parseAsStatement
import org.slf4j.LoggerFactory
import org.springframework.security.access.annotation.Secured
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

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
            val serviceDir = applicationPackageRoot.getOrAddJavaDirectory("service")
            val schema = context.getProject().modules.find { it.name == module.getName() }?.schema

            schema?.entities?.forEach { entity ->
                addControllerClass(entity, restDir, serviceDir, context)
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
        serviceDir: JavaDirectoryNode,
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
                "name" to name,
                "lname" to name.lowerFirst(),
                "primarySetter" to entity.getPrimaryField().getSetterName()
            )
            entity.methods.filter { it.isApi() && it.skipGenerate == true }.forEach { method ->
                values[getBaseName(method.name, entity.name)] = true
            }
            context.writeTemplate(controllerFile, "/templates/spring/EntityApiController.java.vm", values)
            val controllerClass = controllerFile.getRootClass()
            val serviceClass = serviceDir.getClass("${name}Service") ?: error("Service class not found: ${name}Service")
            // secure std method by annotation
            entity.methods.filter { it.isApi() && it.skipGenerate == true }.forEach { methodElem ->
                val method = controllerClass.getMethod(methodElem.name)
                if (method != null) {
                    annotateSecureMethod(method, methodElem)
                }
            }
            entity.methods.filter { it.isApi() && it.skipGenerate != true }.forEach { method ->
                addCustomApiMethod(controllerClass, method, serviceClass)
            }
        } else {
            log.warn("File already exists: {}", fileName)
        }
    }

    private fun addCustomApiMethod(
        clazz: JavaClassNode,
        methodElem: MethodElem,
        serviceClass: JavaClassNode
    ) {
        val serviceField = serviceClass.getName().lowerFirst()
        val serviceMethod = serviceClass.getMethod(methodElem.name)
            ?: error("Service method not found: ${methodElem.name}")
        val method = clazz.addMethod(methodElem.name)
            .withModifiers(Modifier.Keyword.PUBLIC)
            .setReturnType(serviceMethod.getTypeAsString())
        // TODO: support Principal for userDto/userId parameter
        val arguments = mutableListOf<String>()
        val serviceArgs = serviceMethod.getParameters()
        var paramIndex = 0
        check(serviceArgs.size == methodElem.args?.size) { "Service method args count mismatch: ${serviceArgs.size} != ${methodElem.args?.size}" }
        methodElem.args?.forEach { (param, value) ->
            val index = param.indexOf('@')
            val (type, name) = if (index >= 0) {
                param.substring(0, index) to param.substring(index + 1)
            } else {
                "" to param
            }
            when (type) {
                "param" -> {
                    method.addParameter(serviceArgs[paramIndex].getTypeAsString(), name)
                    arguments += name
                    val annotationParams = mutableMapOf<String, Expression>()
                    annotationParams["value"] = StringLiteralExpr(name)
                    annotationParams["required"] = BooleanLiteralExpr(false)
                    if (value != null) {
                        annotationParams["defaultValue"] = StringLiteralExpr(value.toString())
                    }
                    val lastParameter = method.getLastParameter()
                    lastParameter.addAnnotation(RequestParam::class.java, annotationParams)
                }

                "path" -> {
                    check(value == "") { "Path variable value is not supported, but found: '$value'" }
                    method.addParameter(serviceArgs[paramIndex].getTypeAsString(), name)
                    arguments += name
                    val lastParameter = method.getLastParameter()
                    lastParameter.addAnnotation(PathVariable::class.java, name)
                }

                "" -> {
                    arguments += if (value is String) "\"$value\"" else value.toString()
                }
            }
            paramIndex++
        }
        val args = arguments.joinToString(", ")
        method.setBody("return $serviceField.${serviceMethod.getName()}(${args});".parseAsStatement())

        if (methodElem.description?.isNotBlank() == true) {
            method.setJavadocComment(methodElem.description)
        }
        annotateMappingHttpMethod(method, methodElem)
        annotateSecureMethod(method, methodElem)
    }

    private fun annotateMappingHttpMethod(method: JavaMethodNode, methodElem: MethodElem) {
        val httpMethod = methodElem.apiMethod ?: getRequestBYMethodName(method.getName())
        val apiPath = methodElem.apiPath ?: error("apiPath is required for method: ${method.getName()}")
        when (httpMethod) {
            RequestMethod.GET -> method.addAnnotation(GetMapping::class.java, apiPath)
            RequestMethod.POST -> method.addAnnotation(PostMapping::class.java, apiPath)
            RequestMethod.PUT -> method.addAnnotation(PutMapping::class.java, apiPath)
            RequestMethod.DELETE -> method.addAnnotation(DeleteMapping::class.java, apiPath)
            RequestMethod.PATCH -> method.addAnnotation(PatchMapping::class.java, apiPath)
            RequestMethod.HEAD -> method.addAnnotation(
                RequestMapping::class.java,
                "method" to NameExpr("RequestMethod.HEAD"),
                "path" to StringLiteralExpr(apiPath)
            ).addImport(RequestMethod::class.java)

            RequestMethod.OPTIONS -> method.addAnnotation(
                RequestMapping::class.java,
                "method" to NameExpr("RequestMethod.OPTIONS"),
                "path" to StringLiteralExpr(apiPath)
            ).addImport(RequestMethod::class.java)

            RequestMethod.TRACE -> method.addAnnotation(
                RequestMapping::class.java,
                "method" to NameExpr("RequestMethod.TRACE"),
                "path" to StringLiteralExpr(apiPath)
            ).addImport(RequestMethod::class.java)
        }
    }

    private fun annotateSecureMethod(method: JavaMethodNode, methodElem: MethodElem) {
        val roles = methodElem.roles
        if (roles?.isNotEmpty() == true) {
            if (roles.size == 1) {
                method.addAnnotation(
                    Secured::class.java,
                    roles[0]
                )
            } else {
                method.addAnnotation(
                    Secured::class.java,
                    roles.map { StringLiteralExpr(it) }
                )
            }
        }
        val permissions = methodElem.permissions
        if (permissions?.isNotEmpty() == true) {
            if (permissions.size == 1) {
                method.addAnnotation(
                    PreAuthorize::class.java,
                    "hasAuthority('${permissions[0]}')"
                )
            } else {
                method.addAnnotation(
                    PreAuthorize::class.java,
                    permissions.joinToString("', '", "hasAnyAuthority('", "')")
                )
            }
        }
    }

    private fun getRequestBYMethodName(name: String): RequestMethod {
        return when {
            name.startsWith("get") || name.startsWith("find") -> RequestMethod.GET
            name.startsWith("create") -> RequestMethod.POST
            name.startsWith("update") -> RequestMethod.PUT
            name.startsWith("delete") -> RequestMethod.DELETE
            name.startsWith("head") -> RequestMethod.HEAD
            name.startsWith("patch") -> RequestMethod.PATCH
            name.startsWith("options") -> RequestMethod.OPTIONS
            name.startsWith("trace") -> RequestMethod.TRACE
            else -> RequestMethod.GET
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
