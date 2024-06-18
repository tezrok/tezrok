package io.tezrok.search

import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.expr.StringLiteralExpr
import com.github.javaparser.ast.stmt.Statement
import io.tezrok.api.GeneratorContext
import io.tezrok.api.ProcessModelPhase
import io.tezrok.api.TezrokFeature
import io.tezrok.api.input.*
import io.tezrok.api.java.JavaClassNode
import io.tezrok.api.java.JavaDirectoryNode
import io.tezrok.api.maven.ProjectNode
import io.tezrok.util.*
import lombok.Data
import org.jetbrains.annotations.NotNull
import org.slf4j.LoggerFactory
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.TypeAlias
import org.springframework.data.elasticsearch.annotations.Document
import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.annotations.FieldType

/**
 * Adds search dtos and search services (based on Elasticsearch).
 */
internal class SearchableFeature : TezrokFeature {
    override fun apply(project: ProjectNode, context: GeneratorContext): Boolean {
        val module = project.getSingleModule()
        val applicationPackageRoot = module.source.main.java.applicationPackageRoot
        val schemaModule = context.getProject().modules.find { it.name == module.getName() }
            ?: throw IllegalStateException("Module ${module.getName()} not found")
        val entities = schemaModule.schema?.entities ?: emptyList()

        if (applicationPackageRoot != null && schemaModule.isSearchable()) {
            module.pom.addDependency("org.springframework.boot:spring-boot-starter-data-elasticsearch:${'$'}{spring-boot.version}")
            addDynamicIndexRepositories(applicationPackageRoot.getOrAddJavaDirectory("search"), context)

            // add search DTOs
            if (entities.any { it.isSearchable() }) {
                val dtoDir = applicationPackageRoot.getOrAddJavaDirectory("dto")
                    .getOrAddJavaDirectory("search")
                entities.filter { it.isSearchable() }.forEach { entity ->
                    addSearchDto(dtoDir, entity)
                }
            }
        }

        return true
    }

    override fun processModel(project: ProjectElem, phase: ProcessModelPhase): ProjectElem {
        if (phase != ProcessModelPhase.PreProcess) {
            return project
        }

        return project.copy(modules = project.modules.map { processModule(it) })
    }

    private fun addDynamicIndexRepositories(searchDir: JavaDirectoryNode, context: GeneratorContext) {
        context.addFile(searchDir, "/templates/search/DynamicIndexRepository.java.vm")
        context.addFile(searchDir, "/templates/search/DynamicIndexRepositoryImpl.java.vm")
    }

    private fun addSearchDto(dtoDir: JavaDirectoryNode, entity: EntityElem) {
        val entityName = "${entity.name}SearchDto"
        if (!dtoDir.hasClass(entityName)) {
            val esEntityName = entity.name.camelCaseToLowerSnakeCase()
            val indexName = entity.searchEntity?.indexName ?: "${esEntityName}s"
            val dtoClass = dtoDir.addClass(entityName)
                .setJavadocComment("Search DTO for {@link SubjectDto}.")
                .addImport(FieldType::class.java)
                .addAnnotation(Data::class.java)
                .addAnnotation(TypeAlias::class.java, esEntityName)
                .addAnnotation(Document::class.java, "indexName" to StringLiteralExpr(indexName))
                .addImportBySimpleName("${entity.name}Dto")

            entity.fields.filter { it.isNotLogic() && it.isSearchable() }.forEach { field ->
                val newField = dtoClass.addField(field.asJavaType(true), field.name)
                if (field.isPrimary()) {
                    newField.addAnnotation(Id::class.java)
                } else {
                    val fieldType = if (field.type == "String") {
                        "Text"
                    } else if (field.type == "Long") {
                        "Long"
                    } else if (field.type == "Integer") {
                        "Integer"
                    } else if (field.type == "Boolean") {
                        "Boolean"
                    } else if (field.type == "DateTimeTZ" || field.type == "LocalDateTime") {
                        "Date"
                    } else {
                        error("Unsupported field type: ${field.type}")
                    }

                    newField.addAnnotation(
                        Field::class.java,
                        "type" to NameExpr("FieldType.$fieldType"),
                        "name" to StringLiteralExpr(field.name.camelCaseToLowerSnakeCase())
                    )
                }
            }

            addOfMethod(dtoClass, entity)
        } else {
            log.warn("Search DTO already exists: {}", entityName)
        }
    }

    private fun addOfMethod(searchDtoClass: JavaClassNode, entity: EntityElem) {
        val dtoName = "${entity.name}Dto"
        val searchDtoName = searchDtoClass.getName()
        val paramName = entity.name.lowerFirst()
        val method = searchDtoClass.addMethod("of")
            .withModifiers(Modifier.Keyword.PUBLIC, Modifier.Keyword.STATIC)
            .setReturnType(searchDtoClass.getName())
            .addParameter(dtoName, paramName)
            .addAnnotation(NotNull::class.java)
            .setJavadocComment(
                """Create {@link $searchDtoName} from {@link $dtoName}.

@param $paramName source $paramName
@return new {@link $searchDtoName}"""
            )

        val statements = NodeList<Statement>()
        statements.add("final $searchDtoName result = new $searchDtoName();".parseAsStatement())
        entity.fields.filter { it.isNotLogic() && it.isSearchable() }.forEach { field ->
            val setter = field.getSetterName()
            val getter = field.getGetterName()
            statements.add("result.$setter($paramName.$getter());".parseAsStatement())
        }
        statements.add("return result;".parseAsStatement())

        method.setBody(statements)
    }

    private fun processModule(module: ModuleElem): ModuleElem {
        val entities = addExtraEntities(module.schema?.entities?.map { processEntity(it) })
        // make module searchable if at least one entity is searchable
        val moduleSearchable = entities?.ifEmpty { null }?.any { it.isSearchable() } ?: module.searchable
        return module.copy(schema = module.schema?.copy(entities = entities), searchable = moduleSearchable)
    }

    private fun processEntity(entity: EntityElem): EntityElem {
        if (entity.isSearchable()) {
            // if entity is searchable, make all not defined fields searchable
            val newFields = entity.fields.map { if (it.searchable == null) it.copy(searchable = true) else it }
            return entity.copy(fields = newFields)
        }
        val searchableFields = entity.fields.filter { it.isSearchable() }
        if (searchableFields.isNotEmpty()) {
            // if some fields are searchable make entity searchable and make all not defined fields not searchable
            val newFields = entity.fields.map { if (it.searchable == null) it.copy(searchable = false) else it }
            return entity.copy(searchable = true, fields = newFields)
        }

        return entity
    }

    private fun addExtraEntities(entities: List<EntityElem>?): List<EntityElem>? {
        if (entities.isNullOrEmpty()) {
            return entities
        }
        val extraEntities = mutableListOf<EntityElem>()
        entities.filter { it.isSearchable() }.forEach() { entity ->
            extraEntities.add(
                EntityElem(
                    name = "${entity.name}Indexed",
                    description = "Status of ${entity.name} entity in search index",
                    syntheticTo = entity.name,
                    skipController = true,
                    skipService = true,
                    createdAt = true,
                    fields = listOf(
                        FieldElem(
                            "id",
                            ModelTypes.LONG,
                            primary = true
                        ),
                        FieldElem(
                            entity.name.lowerFirst(),
                            entity.name,
                            description = "Reference to ${entity.name} entity",
                            relation = EntityRelation.OneToOne,
                            required = true
                        )
                    )
                )
            )
        }

        return entities.plus(extraEntities)
    }

    private companion object {
        val log = LoggerFactory.getLogger(SearchableFeature::class.java)!!
    }
}
