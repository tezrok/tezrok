package io.tezrok.admin

import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.expr.BooleanLiteralExpr
import com.github.javaparser.ast.expr.StringLiteralExpr
import io.tezrok.api.GeneratorContext
import io.tezrok.api.TezrokFeature
import io.tezrok.api.java.JavaFieldNode
import io.tezrok.api.maven.ProjectNode
import io.tezrok.util.*
import org.springframework.data.domain.Pageable
import org.springframework.expression.spel.ast.BooleanLiteral
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam

class AdminFeature : TezrokFeature {
    override fun apply(project: ProjectNode, context: GeneratorContext): Boolean {
        val module = project.getSingleModule()
        val moduleElem = context.getProject().modules.find { it.name == module.getName() }
            ?: error("Module ${module.getName()} not found")
        val appPackageRoot = module.source.main.java.applicationPackageRoot

        if (appPackageRoot != null) {
            val adminDir = appPackageRoot.getOrAddJavaDirectory("web").getOrAddJavaDirectory("admin")
            val controller = adminDir.addJavaFile("AdminController.java")
            context.writeTemplate(controller, "/templates/admin/AdminController.java.vm")
            val clazz = controller.getRootClass()
            val entities = moduleElem.schema?.entities?.filter { it.isNotSynthetic() && it.hasFullDto() }
            val fields = mutableListOf<JavaFieldNode>()
            entities?.forEach { entity ->
                val serviceName = entity.getServiceName()
                val field = clazz.addField(serviceName, serviceName.lowerFirst())
                field.setModifiers(Modifier.Keyword.FINAL)
                fields.add(field)
            }
            fields.forEach(clazz::initInConstructor)

            entities?.forEach { entity ->
                val serviceField = entity.getServiceName().lowerFirst()
                val pluralName = (entity.name + "s").lowerFirst()
                val method = clazz.addMethod("show${pluralName.upperFirst()}")
                    .withModifiers(Modifier.Keyword.PUBLIC)
                    .setReturnType("String")
                    .addAnnotation(GetMapping::class.java, "/${pluralName}")
                val primaryKey = entity.getPrimaryField().name
                val dtoName = entity.getDtoName()
                clazz.addImportBySimpleName(dtoName)

                method.addParameter("String", "searchTerm")
                method.getLastParameter()
                    .addAnnotation(RequestParam::class.java, "value" to StringLiteralExpr("searchTerm"), "required" to BooleanLiteralExpr(false))

                method.addParameter("Map<String, Object>", "model")
                method.addParameter(Pageable::class.java, "pageable")
                method.setBody(
                    """
final Sort sort = pageable.getSortOr(Sort.by("$primaryKey"));
final Pageable finalPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
final Page<$dtoName> result = $serviceField.searchByTerm(StringUtils.defaultString(searchTerm).trim(), finalPageable, MatchType.CONTAINS, true);
model.put("$pluralName", result);
return "admin/$pluralName";""".parseAsBlock()
                );
            }
        }
        return true
    }
}
