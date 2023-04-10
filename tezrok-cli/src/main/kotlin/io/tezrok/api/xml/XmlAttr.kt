package io.tezrok.api.xml

import org.apache.commons.lang3.Validate

open class XmlAttr(private val name: String, private val value: String) {
    fun getName(): String = name

    fun  getValue(): String = value

    init {
        Validate.notBlank(name, "Xml attribute name cannot be blank")
    }
}
