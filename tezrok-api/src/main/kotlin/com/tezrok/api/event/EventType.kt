package com.tezrok.api.event

import org.apache.commons.lang3.Validate
import com.tezrok.api.node.Node

/**
 * Event type of [Node]
 *
 * @see NodeEvent
 */
data class EventType(val name: String) {
    init {
        Validate.notBlank(name, "Name cannot be blank")
    }

    companion object {
        @JvmField
        val PreAdd = of("PreAdd")

        @JvmField
        val PostAdd = of("PostAdd")

        @JvmField
        val PreEdit = of("PreEdit")

        @JvmField
        val PostEdit = of("PostEdit")

        @JvmField
        val PreDelete = of("PreDelete")

        @JvmField
        val PostDelete = of("PostDelete")

        @JvmField
        val All = listOf(PreAdd, PostAdd, PreEdit, PostEdit, PreDelete, PostDelete)

        /**
         * Creates new [EventType]
         */
        @JvmStatic
        fun of(name: String): EventType = EventType(name)

        /**
         * Gets known [EventType] by name or null
         */
        @JvmStatic
        fun get(name: String): EventType? = cache[name]

        /**
         * Gets known [EventType] by name or creates new
         */
        @JvmStatic
        fun getOrCreate(name: String): EventType = get(name) ?: of(name)

        private val cache: Map<String, EventType> = All.associateBy { it.name }
    }
}
