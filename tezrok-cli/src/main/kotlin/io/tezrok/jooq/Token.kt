package io.tezrok.jooq

/**
 * Represents a token in custom jooq methods.
 *
 * @see MethodExpressionParser
 */
abstract class Token(open val name: String) {
    data class Name(override val name: String, val ignoreCase: Boolean = false) : Token(name) {
        fun decapitalize(): Name = this.copy(name = name.decapitalize())

        fun ignoreCase(ignoreCase: Boolean = true): Name = this.copy(ignoreCase = ignoreCase)
    }

    data class SortName(override val name: String, val sort: Sort = Sort.Default) : Token(name)

    data class Top(val limit: Int = 1) : Token("Top")

    object OrderBy : Token("OrderBy")

    object StartingWith : Token("StartingWith")

    object EndingWith : Token("EndingWith")

    object Containing : Token("Containing")

    object Like : Token("Like")

    object Between : Token("Between")

    object Before : Token("Before")

    object After : Token("After")

    object GreaterThan : Token("GreaterThan")

    object GreaterThanEqual : Token("GreaterThanEqual")

    object LessThan : Token("LessThan")

    object LessThanEqual : Token("LessThanEqual")

    object Distinct : Token("Distinct")

    object And : Token("And")

    object Or : Token("Or")

    object Is : Token("Is")

    object In : Token("In")

    object Equals : Token("Equals")

    object Equal : Token("Equal")

    object IsNot : Token("IsNot")

    object Null : Token("Null")

    object Not : Token("Not")

    enum class Sort {
        Default,
        Asc,
        Desc
    }

    override fun toString(): String = "Token($name)"
}
