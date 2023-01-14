package com.tezrok.core.tree

import com.tezrok.api.tree.Node
import com.tezrok.api.tree.NodeRef
import com.tezrok.api.tree.PropertyValue
import com.tezrok.core.util.JsonUtil
import java.time.OffsetDateTime
import java.util.*

/**
 * Implementation of [PropertyValue] for [List]
 */
internal class ListPropertyValue : PropertyValue {
    private val mapper = JsonUtil.createMapper()

    override fun fromString(value: String): Any? {
        val listType = mapper.typeFactory.constructCollectionType(MutableList::class.java, String::class.java)
        return Collections.unmodifiableList(mapper.readValue(value, listType) as MutableList<String>)
    }

    override fun asString(value: Any): String = mapper.writeValueAsString(value)
}

/**
 * Implementation of [PropertyValue] for [OffsetDateTime]
 *
 * TODO: add tests when locales are different
 */
internal class OffsetDateTimePropertyValue : PropertyValue {
    override fun fromString(value: String): Any? = OffsetDateTime.parse(value)

    override fun asString(value: Any): String = (value as OffsetDateTime).toString()
}

/**
 * Implementation of [PropertyValue] for [NodeRef]
 */
internal class NodeRefPropertyValue(private val handler: (String) -> Node?) : PropertyValue {
    override fun fromString(value: String): Any = NodeRefImpl(value, handler)

    override fun asString(value: Any): String = (value as NodeRef).getPath()
}
