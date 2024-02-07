package io.tezrok.jooq

import io.tezrok.jooq.Token.*
import io.tezrok.util.camelCaseToSnakeCase
import io.tezrok.util.lowerFirst

/**
 * Parses method name into tokens.
 *
 * Example: "NameAndAgeOrderByAgeDesc" -> [Name("name"), And, Name("age"), OrderBy, SortName("age", Sort.Desc)]
 */
object MethodExpressionParser {
    fun parse(name: String): List<Token> {
        val tokens = mutableListOf<Token>()
        val parts = name.camelCaseToSnakeCase().split("_").map { map[it] ?: Name(it) }
        var index = 0
        val addNamePart = { part: Name ->
            when (val last = tokens.lastOrNull()) {
                is OrderBy -> tokens.add(SortName(part.name.lowerFirst()))

                is Name -> tokens[tokens.size - 1] = last.copy(name = last.name + part.name)

                is SortName -> tokens[tokens.size - 1] = last.copy(name = last.name + part.name)

                else -> tokens.add(part.decapitalize())
            }
        }
        var allIgnoreCaseUsed = false

        while (index < parts.size) {
            val part = parts[index]
            val nextPart = if (index + 1 < parts.size) parts[index + 1].name else null

            if (part is Name) {
                val lastToken = tokens.lastOrNull()
                when (part.name) {
                    NAME_ORDER -> {
                        if (nextPart == NAME_BY) {
                            if (tokens.isEmpty()) {
                                // probably field name is "OrderBy"
                                addNamePart(part.copy(name = part.name + NAME_BY))
                            } else {
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
                        // ignore "By" after "Top" or "Distinct"
                        if (lastToken !is Top && lastToken !is Distinct) {
                            addNamePart(part)
                        }
                    }

                    NAME_STARTING -> {
                        if (nextPart == NAME_WITH) {
                            if (lastToken is Name || lastToken is Not) {
                                tokens.add(StartingWith)
                            } else {
                                // probably field name is "StartingWith"
                                addNamePart(part.copy(name = part.name + NAME_WITH))
                            }
                            index++
                        } else {
                            addNamePart(part)
                        }
                    }

                    NAME_ENDING -> {
                        if (nextPart == NAME_WITH) {
                            if (lastToken is Name || lastToken is Not) {
                                tokens.add(EndingWith)
                            } else {
                                // probably field name is "EndingWith"
                                addNamePart(part.copy(name = part.name + NAME_WITH))
                            }
                            index++
                        } else {
                            addNamePart(part)
                        }
                    }

                    NAME_WITH -> addNamePart(part)

                    NAME_CONTAINING -> {
                        if (lastToken is Name || lastToken is Not) {
                            tokens.add(Containing)
                        } else {
                            // probably field name is "Containing"
                            addNamePart(part)
                        }
                    }

                    NAME_LIKE -> {
                        if (lastToken is Name || lastToken is Not) {
                            tokens.add(Like)
                        } else {
                            // probably field name is "Like"
                            addNamePart(part)
                        }
                    }

                    NAME_GREATER -> {
                        if (nextPart == NAME_THAN) {
                            if (lastToken is Name) {
                                tokens.add(GreaterThan)
                            } else {
                                // probably field name is "GreaterThan"
                                addNamePart(part.copy(name = part.name + NAME_THAN))
                            }
                            index++
                        } else {
                            // probably field name is  "Greater"
                            addNamePart(part)
                        }
                    }

                    NAME_LESS -> {
                        if (nextPart == NAME_THAN) {
                            if (lastToken is Name) {
                                tokens.add(LessThan)
                            } else {
                                // probably field name is "LessThan"
                                addNamePart(part.copy(name = part.name + NAME_THAN))
                            }
                            index++
                        } else {
                            // probably field name is  "Less"
                            addNamePart(part)
                        }
                    }

                    NAME_EQUAL -> {
                        when (lastToken) {
                            is Name -> {
                                // equals is equivalent to "is"
                                tokens.add(Is)
                            }

                            is GreaterThan -> {
                                tokens[tokens.size - 1] = GreaterThanEqual
                            }

                            is LessThan -> {
                                tokens[tokens.size - 1] = LessThanEqual
                            }

                            else -> {
                                // probably field name is "Equal"
                                addNamePart(part)
                            }
                        }
                    }

                    NAME_NOT -> {
                        if (lastToken is Is) {
                            tokens[tokens.size - 1] = IsNot
                        } else if (nextPart != null && allowedNotSuffixes.contains(nextPart)) {
                            tokens.add(Not)
                        } else {
                            addNamePart(part)
                        }
                    }

                    NAME_ASC,
                    NAME_DESC -> {
                        if (lastToken is SortName) {
                            val sort = if (part.name == NAME_ASC) Sort.Asc else Sort.Desc
                            tokens[tokens.size - 1] = lastToken.copy(sort = sort)
                        } else {
                            addNamePart(part)
                        }
                    }

                    NAME_DISTINCT -> {
                        if (tokens.isEmpty()) {
                            tokens.add(Distinct)
                        } else {
                            // probably field name is "Distinct"
                            addNamePart(part)
                        }
                    }

                    NAME_IGNORE -> {
                        if (nextPart == NAME_CASE) {
                            if (lastToken is Name) {
                                tokens[tokens.size - 1] = lastToken.ignoreCase()
                            } else {
                                // probably field name is "IgnoreCase"
                                addNamePart(part.copy(name = part.name + NAME_CASE))
                            }
                            index++
                        } else {
                            // probably field name is "Ignore"
                            addNamePart(part)
                        }
                    }

                    NAME_ALL -> {
                        if (nextPart == NAME_IGNORE) {
                            val nextNextPart = if (index + 2 < parts.size) parts[index + 2].name else null
                            if (nextNextPart == NAME_CASE) {
                                if (lastToken is Name) {
                                    check(!allIgnoreCaseUsed) { "AllIgnoreCase should be used only once and at the end" }
                                    allIgnoreCaseUsed = true
                                    for ((idx, token) in tokens.withIndex()) {
                                        if (token is Name) {
                                            tokens[idx] = token.ignoreCase()
                                        }
                                    }
                                } else {
                                    // probably field name is "AllIgnoreCase"
                                    addNamePart(part.copy(name = part.name + NAME_IGNORE + NAME_CASE))
                                }
                                index++
                            } else {
                                // probably field name is "AllIgnore"
                                addNamePart(part.copy(name = part.name + NAME_IGNORE))
                            }
                            index++
                        } else {
                            // probably field name is "All"
                            addNamePart(part)
                        }
                    }

                    else -> {
                        if (part.name.startsWith(NAME_TOP)) {
                            val limit = part.name.substring(NAME_TOP.length)
                                .let { if (it.isEmpty()) 1 else it.toIntOrNull() }
                            check(limit != null) { "Top value should be valid int, but found: ${part.name}" }
                            check(tokens.isEmpty()) { "Top should be used only once and at the beginning" }

                            tokens.add(Top(limit))
                        } else {
                            addNamePart(part)
                        }
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

    private const val NAME_ORDER = "Order"
    private const val NAME_STARTING = "Starting"
    private const val NAME_ENDING = "Ending"
    private const val NAME_CONTAINING = "Containing"
    private const val NAME_LESS = "Less"
    private const val NAME_GREATER = "Greater"
    private const val NAME_THAN = "Than"
    private const val NAME_EQUAL = "Equal"
    private const val NAME_LIKE = "Like"
    private const val NAME_WITH = "With"
    private const val NAME_BETWEEN = "Between"
    private const val NAME_AFTER = "After"
    private const val NAME_BEFORE = "Before"
    private const val NAME_DISTINCT = "Distinct"
    private const val NAME_BY = "By"
    private const val NAME_NOT = "Not"
    private const val NAME_ASC = "Asc"
    private const val NAME_DESC = "Desc"
    private const val NAME_TOP = "Top"
    private const val NAME_IN = "In"
    private const val NAME_IGNORE = "Ignore"
    private const val NAME_CASE = "Case"
    private const val NAME_ALL = "All"
    private val map = mapOf(
        "Is" to Is,
        "Equals" to Equals,
        "Null" to Null,
        "And" to And,
        "Or" to Or,
        NAME_BETWEEN to Between,
        NAME_IN to In,
        NAME_BEFORE to Before,
        NAME_AFTER to After,
        NAME_DISTINCT to Distinct,
    )

    private val allowedNotSuffixes = setOf(
        NAME_STARTING, NAME_CONTAINING, NAME_ENDING, NAME_LIKE, NAME_BETWEEN, NAME_IN
    )
}
