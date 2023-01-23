package com.tezrok.api.type

import org.apache.commons.lang3.Validate

/**
 * Simple type definition
 */
class SimpleType private constructor(private val name: String) : BaseType {
    init {
        Validate.notBlank(name, "Name cannot be blank")
    }

    override fun getName(): String = name

    companion object {
        @JvmField
        val String = of("String")

        @JvmField
        val Integer = of("Integer")

        @JvmField
        val Long = of("Long")

        @JvmField
        val Float = of("Float")

        @JvmField
        val Double = of("Double")

        @JvmField
        val Boolean = of("Boolean")

        @JvmField
        val Date = of("Date")

        @JvmField
        val DateTime = of("DateTime")

        @JvmField
        val Time = of("Time")

        @JvmField
        val File = of("File")

        @JvmField
        val All = listOf(String, Integer, Long, Float, Double, Boolean, Date, DateTime, Time, File)

        /**
         * Creates new [SimpleType]
         */
        @JvmStatic
        fun of(name: String): SimpleType = SimpleType(name)

        /**
         * Gets known [SimpleType] by name or null
         */
        @JvmStatic
        fun get(name: String): SimpleType? = cache[name]

        /**
         * Gets known [SimpleType] by name or creates new
         */
        @JvmStatic
        fun getOrCreate(name: String): SimpleType = get(name) ?: of(name)

        private val cache: Map<String, SimpleType> = All.associateBy { it.name }
    }

    override fun toString(): String = "SimpleType(name='$name')"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SimpleType) return false

        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int = name.hashCode()
}
