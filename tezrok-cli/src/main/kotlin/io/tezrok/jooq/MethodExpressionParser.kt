package io.tezrok.jooq

import io.tezrok.util.camelCaseToSnakeCase

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
            val nextPart = if (index + 1 < parts.size) parts[index + 1].name else null

            if (part is Name) {
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
                        // ignore "By" after "Top"
                        if (tokens.lastOrNull() !is Top) {
                            addNamePart(part)
                        }
                    }

                    NAME_STARTING -> {
                        if (nextPart == NAME_WITH) {
                            val last = tokens.lastOrNull()
                            if (last is Name || last is Not) {
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
                            val last = tokens.lastOrNull()
                            if (last is Name || last is Not) {
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
                        val last = tokens.lastOrNull()
                        if (last is Name || last is Not) {
                            tokens.add(Containing)
                        } else {
                            // probably field name is "Containing"
                            addNamePart(part)
                        }
                    }

                    NAME_LIKE -> {
                        val last = tokens.lastOrNull()
                        if (last is Name || last is Not) {
                            tokens.add(Like)
                        } else {
                            // probably field name is "Like"
                            addNamePart(part)
                        }
                    }

                    NAME_GREATER -> {
                        if (nextPart == NAME_THAN) {
                            val last = tokens.lastOrNull()
                            if (last is Name) {
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
                            val last = tokens.lastOrNull()
                            if (last is Name) {
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
                        val last = tokens.lastOrNull()
                        when (last) {
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
                        val last = tokens.lastOrNull()
                        if (last is Is) {
                            tokens[tokens.size - 1] = IsNot
                        } else if (nextPart != null && allowedNotSuffixes.contains(nextPart)) {
                            tokens.add(Not)
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


    abstract class Token(open val name: String) {
        override fun toString(): String = "Token($name)"
    }

    data class Name(override val name: String) : Token(name) {
        fun decapitalize(): Name = this.copy(name = name.decapitalize())
    }

    data class SortName(override val name: String, val sort: Sort = Sort.Default) : Token(name)

    data class Top(val limit: Int = 1) : Token(NAME_TOP)

    object OrderBy : Token("OrderBy")

    object StartingWith : Token("StartingWith")

    object EndingWith : Token("EndingWith")

    object Containing : Token(NAME_CONTAINING)

    object Like : Token(NAME_LIKE)

    object Between : Token(NAME_BETWEEN)

    object GreaterThan : Token("GreaterThan")

    object GreaterThanEqual : Token("GreaterThanEqual")

    object LessThan : Token("LessThan")

    object LessThanEqual : Token("LessThanEqual")

    object And : Token("And")

    object Or : Token("Or")

    object Is : Token("Is")

    object Equals : Token("Equals")

    object Equal : Token("Equal")

    object IsNot : Token("IsNot")

    object Null : Token("Null")

    object Not : Token(NAME_NOT)

    enum class Sort {
        Default,
        Asc,
        Desc
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
    private const val NAME_BY = "By"
    private const val NAME_NOT = "Not"
    private const val NAME_ASC = "Asc"
    private const val NAME_DESC = "Desc"
    private const val NAME_TOP = "Top"
    private val map = mapOf(
        "Is" to Is,
        "Equals" to Equals,
        "Null" to Null,
        "And" to And,
        "Or" to Or,
        NAME_BETWEEN to Between
    )

    private val allowedNotSuffixes = setOf(NAME_STARTING, NAME_CONTAINING, NAME_ENDING, NAME_LIKE, NAME_BETWEEN)
}
