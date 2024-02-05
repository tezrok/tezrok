package io.tezrok.util

import io.tezrok.api.input.EntityElem
import io.tezrok.api.java.JavaClassNode
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.util.*

fun JavaClassNode.addImportsByType(vararg types: String) {
    types.forEach { type ->
        when {
            type.startsWith("List<") -> {
                addImport(List::class.java)
            }
            type.startsWith("Optional<") -> {
                addImport(Optional::class.java)
            }
            type.startsWith("Page<") -> {
                addImport(Page::class.java)
            }
            type.startsWith("Set<") -> {
                addImport(Set::class.java)
            }
            type.startsWith("Map<") -> {
                addImport(Map::class.java)
            }
            type.startsWith("Collection<") -> {
                addImport(Collection::class.java)
            }
        }

        when (type) {
            "LocalDateTime" -> {
                addImport(LocalDateTime::class.java)
            }
            "OffsetDateTime" -> {
                addImport(OffsetDateTime::class.java)
            }
            "LocalDate" -> {
                addImport(LocalDate::class.java)
            }
            "Pageable" -> {
                addImport(Pageable::class.java)
            }
        }
    }
}

/**
 * Extract types from comment and add imports for them.
 *
 * Example:  {@link ItemFullDto#otherItems} or {@link OrderDto}.
 */
fun JavaClassNode.addImportsByType(comment: String, entities: Map<String, EntityElem>, packagePath: String) {
    javaDocLinkPat.findAll(comment).forEach { match ->
        val typeOrigin = match.groupValues[1].let { link ->
            if (link.contains("#")) {
                link.substringBefore("#")
            } else {
                link
            }
        }
        val entityName = typeOrigin.removeSuffix("Dto").removeSuffix("Full")
        val entity = entities[entityName]
        if (entity != null) {
            if (typeOrigin.contains("FullDto")) {
                addImport("${packagePath}.dto.full.${typeOrigin}")
            } else {
                addImport("${packagePath}.dto.${typeOrigin}")
            }
        }
    }
}

private val javaDocLinkPat = Regex("\\{@link ([^}]+?)}")
