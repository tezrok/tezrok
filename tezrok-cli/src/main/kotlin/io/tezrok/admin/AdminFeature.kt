package io.tezrok.admin

import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.expr.BooleanLiteralExpr
import com.github.javaparser.ast.expr.StringLiteralExpr
import io.tezrok.api.GeneratorContext
import io.tezrok.api.TezrokFeature
import io.tezrok.api.input.EntityElem
import io.tezrok.api.input.MetaType
import io.tezrok.api.java.JavaDirectoryNode
import io.tezrok.api.java.JavaFieldNode
import io.tezrok.api.maven.ProjectNode
import io.tezrok.util.*
import lombok.Data
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam

class AdminFeature : TezrokFeature {
    override fun apply(project: ProjectNode, context: GeneratorContext): Boolean {
        val module = project.getSingleModule()
        val moduleElem = context.getProject().modules.find { it.name == module.getName() }
            ?: error("Module ${module.getName()} not found")
        val appPackageRoot = module.source.main.java.applicationPackageRoot

        if (appPackageRoot != null) {
            val thymeleafDir = module.source.main.resources.getOrAddDirectory("templates/thymeleaf/admin")
            context.addFile(thymeleafDir, "/templates/admin/list.html")
            context.addFile(thymeleafDir, "/templates/admin/index.html")
            val adminDir = appPackageRoot.getOrAddJavaDirectory("web/admin")
            val entities = moduleElem.schema?.entities?.filter { it.isNotSynthetic() && it.hasFullDto() } ?: emptyList()
            addAdminController(
                adminDir,
                entities,
                context
            )
        }
        return true
    }

    private fun addAdminController(
        adminDir: JavaDirectoryNode,
        entities: List<EntityElem>,
        context: GeneratorContext
    ) {
        val controller = adminDir.addJavaFile("AdminController.java")
        context.writeTemplate(controller, "/templates/admin/AdminController.java.vm")
        val clazz = controller.getRootClass()
        val fields = mutableListOf<JavaFieldNode>()
        entities.forEach { entity ->
            val serviceName = entity.getServiceName()
            val field = clazz.addField(serviceName, serviceName.lowerFirst())
            field.setModifiers(Modifier.Keyword.PRIVATE, Modifier.Keyword.FINAL)
            fields.add(field)
        }
        fields.forEach(clazz::initInConstructor)

        // add admin main page
        val listOfEntities = entities.map { entity ->
            val serviceField = entity.getServiceName().lowerFirst()
            val pluralName = entity.name + "s"
            val path = pluralName.toHyphenName()
            "new EntityInfo(\"$path\", \"$pluralName\", \"${entity.description ?: ""}\", $serviceField.count())"
        }.joinToString(separator = ",\n")

        clazz.addMethod("admin")
            .withModifiers(Modifier.Keyword.PUBLIC)
            .addParameter("Map<String, Object>", "model")
            .setReturnType("String")
            .addAnnotation(GetMapping::class.java)
            .setBody(
                """
model.put("entities", Arrays.asList(
$listOfEntities
));
return "admin/index";""".parseAsBlock()
            )

        // add entities methods
        entities.forEach { entity ->
            val serviceField = entity.getServiceName().lowerFirst()
            val pluralName = entity.name + "s"
            val path = pluralName.toHyphenName()
            val method = clazz.addMethod("show$pluralName")
                .withModifiers(Modifier.Keyword.PUBLIC)
                .setReturnType("String")
                .addAnnotation(GetMapping::class.java, "/${path}")
            val primaryKey = entity.getPrimaryField().name
            val dtoName = entity.getDtoName()
            clazz.addImportBySimpleName(dtoName)

            val fields = entity.fields
                .filter { field -> field.isNotLogic() && !field.hasMetaType(MetaType.Sensitive) }
                .map { field -> field.name.upperFirst() }
                .joinToString(", ") { "\"$it\"" }

            method.addParameter("String", "searchTerm")
            method.getLastParameter()
                .addAnnotation(
                    RequestParam::class.java,
                    "value" to StringLiteralExpr("searchTerm"),
                    "required" to BooleanLiteralExpr(false)
                )

            method.addParameter("Map<String, Object>", "model")
            method.addParameter(Pageable::class.java, "pageable")
            method.setBody(
                """
    final Sort sort = pageable.getSortOr(Sort.by("$primaryKey"));
    final Pageable finalPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    final Page<$dtoName> result = $serviceField.searchByTerm(StringUtils.defaultString(searchTerm).trim(), finalPageable, MatchType.CONTAINS, true);
    model.put("page", result);
    model.put("columns", Arrays.asList($fields));
    model.put("entityPlural", "$pluralName");
    model.put("path", "$path");
    return "admin/list";""".parseAsBlock()
            )
        }

        // add inner class
        val infoClass = clazz.addInnerClass("EntityInfo")
            .withModifiers(Modifier.Keyword.PRIVATE, Modifier.Keyword.STATIC)
            .addAnnotation(Data::class.java)
        infoClass.addField("String", "id").withModifiers(Modifier.Keyword.PRIVATE, Modifier.Keyword.FINAL)
        infoClass.addField("String", "name").withModifiers(Modifier.Keyword.PRIVATE, Modifier.Keyword.FINAL)
        infoClass.addField("String", "description").withModifiers(Modifier.Keyword.PRIVATE, Modifier.Keyword.FINAL)
        infoClass.addField("Long", "count").withModifiers(Modifier.Keyword.PRIVATE, Modifier.Keyword.FINAL)
    }
}
