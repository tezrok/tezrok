package io.tezrok.spring

import io.tezrok.api.GeneratorContext
import io.tezrok.api.ProcessModelPhase
import io.tezrok.api.TezrokFeature
import io.tezrok.api.input.*
import io.tezrok.api.maven.ProjectNode
import org.slf4j.LoggerFactory

/**
 * Creates spring security configuration and related entities.
 */
open class SpringSecurityFeature : TezrokFeature {
    override fun apply(project: ProjectNode, context: GeneratorContext): Boolean {
        val module = project.getSingleModule()
        val pom = module.pom
        pom.addDependency("org.springframework.boot:spring-boot-starter-security:${'$'}{spring-boot.version}")
        val applicationPackageRoot = module.source.main.java.applicationPackageRoot

        if (applicationPackageRoot != null) {
            val projectElem = context.getProject()
            val values = mapOf("package" to projectElem.packagePath)
            val userDetails = applicationPackageRoot.getOrAddJavaDirectory("dto").addJavaFile("UserDetailsImpl")
            context.writeTemplate(userDetails, "/templates/spring/security/UserDetailsImpl.java.vm", values)
            val userDetailsService = applicationPackageRoot.getOrAddJavaDirectory("service").addJavaFile("UserDetailsServiceImpl")
            context.writeTemplate(userDetailsService, "/templates/spring/security/UserDetailsServiceImpl.java.vm", values)
        }

        return true
    }

    override fun processModel(project: ProjectElem, phase: ProcessModelPhase): ProjectElem {
        if (phase != ProcessModelPhase.PreProcess) {
            // we need only PreProcess phase to add Auth entities
            return project
        }

        return project.copy(modules = project.modules.map { module -> processModule(module) })
    }

    private fun processModule(module: ModuleElem): ModuleElem {
        val auth = module.auth
        if (auth != null) {
            val schema = module.schema ?: SchemaElem()
            val entities = schema.entities?.associate { it.name to it }?.toMutableMap() ?: mutableMapOf()
            entities[NAME_USER] = createUser(entities[NAME_USER])
            entities[NAME_ROLE] = createRole(entities[NAME_ROLE])
            entities[NAME_PERMISSION] = createPermission(entities[NAME_PERMISSION])

            return module.copy(schema = schema.copy(entities = entities.values.toList()))
        }

        return module
    }

    private fun createUser(inheritEntity: EntityElem?): EntityElem {
        return EntityElem(
            name = NAME_USER,
            description = "User entity",
            customRepository = true,
            customMethods = (inheritEntity?.customMethods ?: emptySet()) + "getByNameOrEmail",
            fields = createUserFields(inheritEntity)
        )
    }

    private fun createRole(inheritEntity: EntityElem?): EntityElem {
        return EntityElem(
            name = NAME_ROLE,
            description = "Role entity",
            customRepository = true,
            customMethods = inheritEntity?.customMethods,
            fields = createRoleFields(inheritEntity)
        )
    }

    private fun createPermission(inheritEntity: EntityElem?): EntityElem {
        return EntityElem(
            name = NAME_PERMISSION,
            description = "Permission entity",
            customRepository = true,
            customMethods = (inheritEntity?.customMethods ?: emptySet()) + "findRolePermissionsByRoleIdIn",
            fields = createPermissionFields(inheritEntity)
        )
    }

    private fun createUserFields(inheritEntity: EntityElem?): List<FieldElem> {
        return mergeFields(
            inheritEntity, listOf(
                FieldElem(name = "id", type = "long", primary = true),
                FieldElem(
                    name = "name",
                    type = "string",
                    required = true,
                    unique = true,
                    maxLength = USER_NAME_MAX,
                    minLength = USER_NAME_MIN
                ),
                FieldElem(
                    name = "email",
                    type = "string",
                    required = true,
                    unique = true,
                    maxLength = USER_EMAIL_MAX,
                    minLength = USER_EMAIL_MIN
                ),
                FieldElem(name = "password", type = "string", maxLength = USER_PASSWORD_HASH_MAX, required = true),
                FieldElem(name = "activated", type = "boolean", required = true, defValue = "false"),
                FieldElem(name = "banned", type = "boolean", required = true, defValue = "false"),
                FieldElem(name = "createdAt", type = "dateTime", required = true, defValue = "now()"),
                FieldElem(name = "updatedAt", type = "dateTime", required = true, defValue = "now()"),
                FieldElem(name = "roles", type = "Role", relation = EntityRelation.ManyToMany)
            )
        )
    }

    private fun createRoleFields(inheritEntity: EntityElem?): List<FieldElem> {
        return mergeFields(
            inheritEntity, listOf(
                FieldElem(name = "id", type = "long", primary = true),
                FieldElem(
                    name = "name",
                    type = "string",
                    required = true,
                    unique = true,
                    maxLength = USER_NAME_MAX,
                    minLength = USER_NAME_MIN
                ),
                FieldElem(
                    name = "description",
                    type = "string",
                    maxLength = DESCRIPTION_MAX,
                    minLength = DESCRIPTION_MIN
                ),
                FieldElem(name = "createdAt", type = "dateTime", required = true),
                FieldElem(name = "updatedAt", type = "dateTime", required = true),
                FieldElem(name = "permissions", type = "Permission", relation = EntityRelation.ManyToMany)
            )
        )
    }

    private fun createPermissionFields(inheritEntity: EntityElem?): List<FieldElem> {
        return mergeFields(
            inheritEntity, listOf(
                FieldElem(name = "id", type = "long", primary = true),
                FieldElem(
                    name = "name",
                    type = "string",
                    required = true,
                    unique = true,
                    maxLength = USER_NAME_MAX,
                    minLength = USER_NAME_MIN
                ),
                FieldElem(
                    name = "description",
                    type = "string",
                    maxLength = DESCRIPTION_MAX,
                    minLength = DESCRIPTION_MIN
                ),
                FieldElem(name = "createdAt", type = "dateTime", required = true),
                FieldElem(name = "updatedAt", type = "dateTime", required = true)
            )
        )
    }

    private fun mergeFields(inheritEntity: EntityElem?, fields: List<FieldElem>): List<FieldElem> {
        val map = inheritEntity?.fields?.associateBy { it.name }?.toMutableMap() ?: mutableMapOf()

        fields.forEach { field -> map[field.name] = inheritProperties(field, map[field.name]) }

        return map.values.toList()
    }

    private fun inheritProperties(field: FieldElem, fieldFrom: FieldElem?): FieldElem {
        if (fieldFrom == null) {
            return field
        }

        return field.copy(
            type = field.type ?: fieldFrom.type,
            foreignField = field.foreignField ?: fieldFrom.foreignField,
            description = field.description ?: fieldFrom.description,
            required = field.required ?: fieldFrom.required,
            serial = field.serial ?: fieldFrom.serial,
            primary = field.primary ?: fieldFrom.primary,
            pattern = field.pattern ?: fieldFrom.pattern,
            minLength = field.minLength ?: fieldFrom.minLength,
            maxLength = field.maxLength ?: fieldFrom.maxLength,
            unique = field.unique ?: fieldFrom.unique,
            uniqueGroup = field.uniqueGroup ?: fieldFrom.uniqueGroup,
            defValue = field.defValue ?: fieldFrom.defValue,
            relation = field.relation ?: fieldFrom.relation
        )
    }

    private companion object {
        val log = LoggerFactory.getLogger(SpringSecurityFeature::class.java)
        const val NAME_USER = "User"
        const val NAME_ROLE = "Role"
        const val NAME_PERMISSION = "Permission"
        const val USER_NAME_MIN: Int = 3
        const val USER_NAME_MAX: Int = 20
        const val USER_PASSWORD_MIN: Int = 5
        const val USER_PASSWORD_MAX: Int = 30
        const val USER_PASSWORD_HASH_MAX: Int = 100
        const val USER_EMAIL_MIN: Int = 4
        const val USER_EMAIL_MAX: Int = 40
        const val DESCRIPTION_MIN: Int = 0
        const val DESCRIPTION_MAX: Int = 150
    }
}
