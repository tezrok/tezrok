package io.tezrok.jooq

import io.tezrok.util.camelCaseToSnakeCase

/**
 * Parses method name into tokens.
 *
 * Example: "findByNameAndAgeOrderByAgeDesc" -> [Name("name"), And, Name("age"), OrderBy, SortName("age", Sort.Desc)]
 */
object MethodExpressionParser {
    fun parse(name: String): List<Token> {
        val tokens = mutableListOf<Token>()
        val parts = name.camelCaseToSnakeCase().split("_").map { map[it] ?: Name(it) }
        var index = 0
        val addNamePart = { part: Name ->
            when (val last = tokens.lastOrNull()) {
                is OrderBy -> {
                    tokens.add(SortName(part.name.decapitalize()))
                }

                is Name -> {
                    tokens[tokens.size - 1] = last.copy(name = last.name + part.name)
                }

                is SortName -> {
                    tokens[tokens.size - 1] = last.copy(name = last.name + part.name)
                }

                else -> {
                    tokens.add(part.decapitalize())
                }
            }
        }

        while (index < parts.size) {
            val part = parts[index]

            if (part is Name) {
                when (part.name) {
                    NAME_ORDER -> {
                        if (index + 1 < parts.size && parts[index + 1].name == NAME_BY) {
                            if (tokens.isEmpty()) {
                                // probably field name is "OrderBy"
                                addNamePart(part.copy(name = part.name + NAME_BY))
                            } else {
                                check(tokens.lastOrNull() is Name) { "Order by should be preceded by field name" }
                                check(tokens.find { it is OrderBy } == null) { "Order by should be used only once" }
                                tokens.add(OrderBy)
                            }
                            index++
                        } else {
                            // probably field name is "Order"
                            addNamePart(part)
                        }
                    }

                    NAME_BY -> {
                        addNamePart(part)
                    }

                    NAME_NOT -> {
                        if (tokens.lastOrNull() is Is) {
                            tokens[tokens.size - 1] = IsNot
                        } else {
                            addNamePart(part)
                        }
                    }

                    NAME_ASC,
                    NAME_DESC -> {
                        val last = tokens.lastOrNull()
                        if (last is SortName) {
                            val sort = if (part.name == NAME_ASC) Sort.Asc else Sort.Desc
                            tokens[tokens.size - 1] = last.copy(sort = sort)
                        } else {
                            addNamePart(part)
                        }
                    }

                    else -> {
                        addNamePart(part)
                    }
                }
            } else if (part is Equals) {
                // equals is equivalent to "is"
                tokens.add(Is)
            } else {
                tokens.add(part)
            }

            index++
        }


        return tokens
    }


    abstract class Token(open val name: String)

    data class Name(override val name: String) : Token(name) {
        fun decapitalize(): Name = this.copy(name = name.decapitalize())
    }

    data class SortName(override val name: String, val sort: Sort = Sort.Default) : Token(name)

    object OrderBy : Token("OrderBy")

    object And : Token("And")

    object Or : Token("Or")

    object Is : Token("Is")

    object Equals : Token("Equals")

    object IsNot : Token("IsNot")

    object Null : Token("Null")

    enum class Sort {
        Default,
        Asc,
        Desc
    }

    private const val NAME_ORDER = "Order"
    private const val NAME_BY = "By"
    private const val NAME_NOT = "Not"
    private const val NAME_ASC = "Asc"
    private const val NAME_DESC = "Desc"
    private val map = mapOf(
        "Is" to Is,
        "Equals" to Equals,
        "Null" to Null,
        "And" to And,
        "Or" to Or
    )
}
