package io.tezrok.spring.generator

import io.tezrok.api.ExecuteContext
import io.tezrok.api.Generator
import io.tezrok.api.GlobalContext
import io.tezrok.api.builder.JavaClassBuilder
import io.tezrok.api.builder.JavaField
import io.tezrok.api.model.maven.Dependency
import io.tezrok.api.model.maven.Pom
import io.tezrok.api.model.node.EntityNode
import io.tezrok.api.model.node.FieldNode
import io.tezrok.api.model.node.ProjectNode
import io.tezrok.api.visitor.EntityClassVisitor
import io.tezrok.api.visitor.LogicModelVisitor
import io.tezrok.api.visitor.MavenVisitor
import io.tezrok.api.visitor.ModelPhase
import io.tezrok.spring.relation.*
import io.tezrok.spring.relation.JoinColumn
import io.tezrok.spring.relation.JoinTable
import io.tezrok.spring.util.NameUtil
import org.slf4j.LoggerFactory
import javax.persistence.*

/**
 * Generates jpa-repositories
 */
class SpringJPAGenerator : Generator, EntityClassVisitor, LogicModelVisitor, MavenVisitor {
    private lateinit var resolver: EntityRelationResolver

    override fun visit(project: ProjectNode, phase: ModelPhase, context: GlobalContext) {
        if (phase == ModelPhase.PostEdit) {
            resolver = EntityRelationResolver(project).init(context)
        }
    }

    override fun execute(context: ExecuteContext) {
        log.warn("execute method not implemented")
    }

    override fun visit(clazz: JavaClassBuilder, node: EntityNode) {
        val tableName = NameUtil.getTableName(node)

        clazz.annotate(Entity::class.java)
                .annotate("Table(name = \"$tableName\")")
                .addImports(Table::class.java)

        node.fields().forEach { fNode ->
            clazz.getField(fNode.name).ifPresent { annotateField(it, fNode) }
        }
    }

    override fun visit(pom: Pom) {
        pom.add(Dependency("org.hibernate.javax.persistence", "hibernate-jpa-2.1-api", "1.0.2.Final"))
    }

    private fun annotateField(field: JavaField, node: FieldNode) {
        val info = resolver.getInfo(node)

        if (info.isBasic) {
            generateBasicColumn(field, info)
        } else if (info.isOneToMany) {
            generateOneToManyColumn(field, info)
        } else if (info.isManyToOne) {
            generateManyToOneColumn(field, info)
        } else if (info.isManyToMany) {
            generateManyToManyColumn(field, info)
        } else if (info.isOneToOne) {
            generateOneToOneColumn(field, info)
        }
    }

    private fun generateBasicColumn(classField: JavaField, info: ColumnInfo) {
        val basicInfo = info as BasicColumnInfo
        val col = basicInfo.column

        if (col.isPrimary) {
            classField.annotate(Id::class.java)

            if (basicInfo.generationType != null) {
                classField.annotate(getGeneratedAnnotation(basicInfo.generationType))
                classField.addImports(GeneratedValue::class.java, GenerationType::class.java)
            }
        }

        if (basicInfo.isEnum) {
            classField.annotate(getEnumAnnotation(basicInfo.enumType))
            classField.addImports(Enumerated::class.java, EnumType::class.java)
        }

        classField.annotate(getColumnAnnotation(col))
        classField.addImports(Column::class.java)
    }

    private fun generateOneToManyColumn(classField: JavaField, info: ColumnInfo) {
        val oneToManyInfo = info as OneToManyColumnInfo

        classField.annotate(getOneToManyAnnotation(oneToManyInfo))
        classField.addImports(OneToMany::class.java)
    }

    private fun generateManyToOneColumn(classField: JavaField, info: ColumnInfo) {
        val manyToOneInfo = info as ManyToOneColumnInfo
        val joinColumn = manyToOneInfo.joinColumn

        classField.annotate(getManyToOneAnnotation(manyToOneInfo))
        classField.annotate(getJoinColumnAnnotation(joinColumn))
        classField.addImports(ManyToOne::class.java, FetchType::class.java, javax.persistence.JoinColumn::class.java)
    }

    private fun generateManyToManyColumn(classField: JavaField, info: ColumnInfo) {
        val manyToManyInfo = info as ManyToManyColumnInfo

        classField.annotate(getManyToManyAnnotation(manyToManyInfo))
        classField.annotate(getJoinTableAnnotation(manyToManyInfo.joinTable))
        classField.addImports(ManyToMany::class.java, FetchType::class.java, javax.persistence.JoinTable::class.java, javax.persistence.JoinColumn::class.java)
    }

    private fun generateOneToOneColumn(classField: JavaField, info: ColumnInfo) {
        val oneToOneInfo = info as OneToOneColumnInfo
        val joinColumn = oneToOneInfo.getJoinColumn()

        classField.annotate(getOneToOneAnnotation(oneToOneInfo))
        classField.annotate(getJoinColumnAnnotation(joinColumn))
        classField.addImports(OneToOne::class.java, FetchType::class.java, javax.persistence.JoinColumn::class.java)
    }

    private fun getJoinColumnAnnotation(joinColumn: JoinColumn): String {
        return "JoinColumn(name = \"" + joinColumn.getName() + "\", nullable = " + joinColumn.isNullable() + ")"
    }

    private fun getManyToOneAnnotation(manyToOneInfo: ManyToOneColumnInfo): String {
        return "ManyToOne(fetch = FetchType." + manyToOneInfo.getFetch() + ", optional = " + manyToOneInfo.getOptional() + ")"
    }


    private fun getJoinTableAnnotation(joinTable: JoinTable): String {
        return "JoinTable(name = \"" + joinTable.getName() + "\",\n" +
                "            joinColumns = { @" + getJoinColumnAnnotation(joinTable.joinColumn) + " },\n" +
                "            inverseJoinColumns = { @" + getJoinColumnAnnotation(joinTable.inverseJoinColumn) + " })"
    }

    private fun getManyToManyAnnotation(manyToManyInfo: ManyToManyColumnInfo): String {
        return "ManyToMany(fetch = FetchType." + manyToManyInfo.fetchType + ")"
    }

    private fun getOneToOneAnnotation(oneToOneInfo: OneToOneColumnInfo): String {
        return "OneToOne(fetch = FetchType." + oneToOneInfo.fetchType + ")"
    }

    private fun getColumnAnnotation(column: BasicColumnInfo.Column): String {
        val result = StringBuilder("Column(name = \"" + column.name + "\"")

        if (column.length != null) {
            result.append(", length = ")
            result.append(column.length)
        }

        if (column.isUnique) {
            result.append(", unique = true")
        }

        if (!column.isNullable) {
            result.append(", nullable = false")
        }

        result.append(")")

        return result.toString()
    }

    private fun getGeneratedAnnotation(generationType: GenerationType): String {
        return "GeneratedValue(strategy = GenerationType.$generationType)"
    }

    private fun getOneToManyAnnotation(oneToManyInfo: OneToManyColumnInfo): String {
        return "OneToMany(mappedBy = \"" + oneToManyInfo.mappedBy.name + "\")"
    }

    private fun getEnumAnnotation(enumType: EnumType): String {
        return "Enumerated(EnumType.$enumType)"
    }

    companion object {
        private val log = LoggerFactory.getLogger(SpringJPAGenerator::class.java)
    }
}
