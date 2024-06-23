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
        val projectElem = context.getProject()
        val moduleElem = projectElem.modules.find { it.name == module.getName() } ?: error("Module not found")

        if (moduleElem.auth != null) {
            val pom = module.pom
            pom.addDependency("org.springframework.boot:spring-boot-starter-security:${'$'}{spring-boot.version}")
            val applicationPackageRoot = module.source.main.java.applicationPackageRoot

            if (applicationPackageRoot != null) {
                val dtoDir = applicationPackageRoot.getOrAddJavaDirectory("dto")
                context.addFile(dtoDir, "/templates/spring/security/UserDetailsImpl.java.vm")
                val serviceDir = applicationPackageRoot.getOrAddJavaDirectory("service")
                context.addFile(serviceDir, "/templates/spring/security/UserDetailsServiceImpl.java.vm")
                val configDir = applicationPackageRoot.getOrAddJavaDirectory("config")
                context.addFile(configDir, "/templates/spring/security/SecurityConfig.java.vm")
                val repositoryDir = applicationPackageRoot.getOrAddJavaDirectory("repository")
                context.addFile(repositoryDir, "/templates/spring/security/PersistentTokenRepositoryImpl.java.vm")
            }
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
            entities[NAME_USER] = createUser(entities[NAME_USER], auth.stdInit == true)
            entities[NAME_ROLE] = createRole(entities[NAME_ROLE], auth.stdInit == true)
            entities[NAME_PERMISSION] = createPermission(entities[NAME_PERMISSION])
            entities[NAME_USER_PROFILE] = createUserProfile(entities[NAME_USER_PROFILE])
            entities[NAME_REMEMBER_ME_TOKEN] = createRememberMeToken(entities[NAME_REMEMBER_ME_TOKEN])

            return module.copy(schema = schema.copy(entities = entities.values.toList()))
        }

        return module
    }

    private fun createUser(inheritEntity: EntityElem?, stdUsers: Boolean): EntityElem {
        val inheritMethods = inheritEntity?.methods ?: emptySet()
        return EntityElem(
            name = NAME_USER,
            description = "User entity",
            customRepository = true,
            createdAt = true,
            updatedAt = true,
            skipService = inheritEntity?.skipService,
            skipController = inheritEntity?.skipController,
            methods = inheritMethods + MethodElem(
                "getByNameOrEmail",
                "Returns {@link ${NAME_USER}Dto} by name or email."
            ),
            fields = createUserFields(inheritEntity),
            init = inheritEntity?.init ?: stdUsersInit(stdUsers)
        )
    }

    private fun createRole(inheritEntity: EntityElem?, stdRoles: Boolean): EntityElem {
        return EntityElem(
            name = NAME_ROLE,
            description = "Role entity",
            customRepository = true,
            createdAt = true,
            updatedAt = true,
            skipService = inheritEntity?.skipService,
            skipController = inheritEntity?.skipController,
            methods = inheritEntity?.methods ?: emptySet(),
            fields = createRoleFields(inheritEntity),
            init = inheritEntity?.init ?: stdRolesInit(stdRoles)
        )
    }

    private fun createPermission(inheritEntity: EntityElem?): EntityElem {
        return EntityElem(
            name = NAME_PERMISSION,
            description = "Permission entity",
            customRepository = true,
            createdAt = true,
            updatedAt = true,
            skipService = inheritEntity?.skipService,
            skipController = inheritEntity?.skipController,
            methods = inheritEntity?.methods ?: emptySet(),
            fields = createPermissionFields(inheritEntity)
        )
    }

    private fun createUserProfile(inheritEntity: EntityElem?): EntityElem {
        return EntityElem(
            name = NAME_USER_PROFILE,
            description = "Additional user data entity",
            customRepository = false,
            createdAt = true,
            updatedAt = true,
            skipService = inheritEntity?.skipService,
            skipController = inheritEntity?.skipController,
            methods = inheritEntity?.methods ?: emptySet(),
            fields = createUserProfileFields(inheritEntity)
        )
    }

    private fun createRememberMeToken(inheritEntity: EntityElem?): EntityElem {
        val inheritMethods = inheritEntity?.methods ?: emptySet()
        return EntityElem(
            name = NAME_REMEMBER_ME_TOKEN,
            description = "Remember me token entity",
            customRepository = false,
            createdAt = true,
            skipService = true,
            skipController = true,
            methods = inheritMethods + MethodElem("findByUsername", "Find by username."),
            fields = createRememberMeTokenFields(inheritEntity)
        )
    }

    /**
     * Returns standard users in csv format.
     */
    private fun stdUsersInit(stdUsers: Boolean): String? {
        if (!stdUsers) {
            return null
        }

        // name, email, password
        return "admin,admin@site.com,admin"
    }

    /**
     * Returns standard roles in csv format.
     */
    private fun stdRolesInit(stdRoles: Boolean): String? {
        if (!stdRoles) {
            return null
        }

        // name, description
        return "ADMIN,Administrator role\nUSER,Authenticated role"
    }

    private fun createUserFields(inheritEntity: EntityElem?): List<FieldElem> {
        return mergeFields(
            inheritEntity, listOf(
                FieldElem(name = "id", type = "Long", primary = true),
                FieldElem(
                    name = "name",
                    type = "String",
                    description = "User login",
                    required = true,
                    unique = true,
                    maxLength = USER_NAME_MAX,
                    minLength = USER_NAME_MIN
                ),
                FieldElem(
                    name = "email",
                    type = "String",
                    description = "User email",
                    required = true,
                    unique = true,
                    maxLength = USER_EMAIL_MAX,
                    minLength = USER_EMAIL_MIN,
                    metaTypes = setOf(MetaType.Email)
                ),
                FieldElem(
                    name = "password",
                    type = "String",
                    description = "Hashed user password",
                    required = true,
                    maxLength = USER_PASSWORD_HASH_MAX,
                    metaTypes = setOf(MetaType.Sensitive)
                ),
                FieldElem(name = "activated", type = "Boolean", required = true, defValue = "false"),
                FieldElem(name = "banned", type = "Boolean", required = true, defValue = "false"),
                FieldElem(name = "roles", type = "Role", relation = EntityRelation.ManyToMany)
            )
        )
    }

    private fun createRoleFields(inheritEntity: EntityElem?): List<FieldElem> {
        return mergeFields(
            inheritEntity, listOf(
                FieldElem(name = "id", type = "Long", primary = true, primaryIdFrom = 10),
                FieldElem(
                    name = "name",
                    type = "String",
                    description = "Role name",
                    required = true,
                    unique = true,
                    maxLength = USER_NAME_MAX,
                    minLength = USER_NAME_MIN
                ),
                FieldElem(
                    name = "description",
                    type = "String",
                    description = "Role description",
                    maxLength = DESCRIPTION_MAX,
                    minLength = DESCRIPTION_MIN
                ),
                FieldElem(name = "permissions", type = "Permission", relation = EntityRelation.ManyToMany)
            )
        )
    }

    private fun createPermissionFields(inheritEntity: EntityElem?): List<FieldElem> {
        return mergeFields(
            inheritEntity, listOf(
                FieldElem(name = "id", type = "Long", primary = true, primaryIdFrom = 100),
                FieldElem(
                    name = "name",
                    type = "String",
                    description = "Permission name",
                    required = true,
                    unique = true,
                    maxLength = USER_NAME_MAX,
                    minLength = USER_NAME_MIN
                ),
                FieldElem(
                    name = "description",
                    type = "String",
                    description = "Permission description",
                    maxLength = DESCRIPTION_MAX,
                    minLength = DESCRIPTION_MIN
                )
            )
        )
    }

    private fun createUserProfileFields(inheritEntity: EntityElem?): List<FieldElem> {
        return mergeFields(
            inheritEntity, listOf(
                FieldElem(name = "id", type = "Long", primary = true, primaryIdFrom = 10),
                FieldElem(name = "user", type = "User", required = true, relation = EntityRelation.OneToOne),
                FieldElem(
                    name = "activationCode",
                    type = "String",
                    description = "Code to activate user account",
                    unique = true,
                    minLength = ACTIVATION_CODE_MIN,
                    maxLength = ACTIVATION_CODE_MAX
                ),
                FieldElem(
                    name = "passwordResetCode",
                    type = "String",
                    description = "Code to reset user password",
                    unique = true,
                    minLength = PASSWORD_RESET_CODE_MIN,
                    maxLength = PASSWORD_RESET_CODE_MAX
                ),
                FieldElem(
                    name = "passwordResetCodeExpireAt",
                    type = "DateTimeTZ",
                    description = "Password reset code expiration date and time"
                )
            )
        )
    }

    private fun createRememberMeTokenFields(inheritEntity: EntityElem?): List<FieldElem> {
        return mergeFields(
            inheritEntity, listOf(
                FieldElem(
                    name = "series",
                    type = "String",
                    description = "Remember me token primary key",
                    required = true,
                    primary = true,
                    maxLength = REMEMBER_ME_SIZE,
                    minLength = 3
                ),
                FieldElem(
                    name = "username",
                    type = "String",
                    description = "Related user name",
                    required = true,
                    maxLength = USER_NAME_MAX,
                    minLength = USER_NAME_MIN
                ), FieldElem(
                    name = "token",
                    type = "String",
                    description = "Remember me token",
                    required = true,
                    maxLength = REMEMBER_ME_SIZE,
                    minLength = 3
                ),
                FieldElem(
                    name = "lastUsed",
                    type = "DateTimeTZ",
                    required = true,
                    defValue = "now()",
                    description = "Last used date and time"
                )
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
            primaryIdFrom = field.primaryIdFrom ?: fieldFrom.primaryIdFrom,
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
        val log = LoggerFactory.getLogger(SpringSecurityFeature::class.java)!!
        const val NAME_USER = "User"
        const val NAME_ROLE = "Role"
        const val NAME_PERMISSION = "Permission"
        const val NAME_USER_PROFILE = "UserProfile"
        const val NAME_REMEMBER_ME_TOKEN = "RememberMeToken"
        const val UUID_LENGTH: Int = 36
        const val REMEMBER_ME_SIZE = 64
        const val USER_NAME_MIN: Int = 3
        const val USER_NAME_MAX: Int = 20
        const val ACTIVATION_CODE_MIN: Int = UUID_LENGTH
        const val ACTIVATION_CODE_MAX: Int = ACTIVATION_CODE_MIN
        const val PASSWORD_RESET_CODE_MIN: Int = UUID_LENGTH
        const val PASSWORD_RESET_CODE_MAX: Int = PASSWORD_RESET_CODE_MIN
        const val USER_PASSWORD_MIN: Int = 5
        const val USER_PASSWORD_MAX: Int = 55
        const val USER_PASSWORD_HASH_MAX: Int = 100
        const val USER_EMAIL_MIN: Int = 4
        const val USER_EMAIL_MAX: Int = 40
        const val DESCRIPTION_MIN: Int = 0
        const val DESCRIPTION_MAX: Int = 150
    }
}
