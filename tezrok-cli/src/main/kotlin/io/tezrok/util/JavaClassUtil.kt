package io.tezrok.util

import io.tezrok.api.java.JavaClassNode
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.LocalDate
import java.time.LocalDateTime
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
            "LocalDate" -> {
                addImport(LocalDate::class.java)
            }
            "Pageable" -> {
                addImport(Pageable::class.java)
            }
        }
    }
}
