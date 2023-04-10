package io.tezrok.api.xml

import org.apache.commons.lang3.Validate
import org.w3c.dom.Element
import java.io.OutputStream
import java.util.*
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

open class XmlNode(private val name: String) {
    fun getName(): String = name

    private val attrs: MutableList<XmlAttr> = ArrayList()
    private val items: MutableList<XmlNode> = ArrayList()

    init {
        Validate.notBlank(name, "Xml node name cannot be blank")
    }

    fun node(name: String): XmlNode {
        val child = XmlNode(name)
        items.add(child)
        return child
    }

    fun attr(name: String, value: String): XmlNode {
        attrs.add(XmlAttr(name, value))
        return this
    }

    val isEmpty: Boolean
        get() = items.isEmpty()

    fun getAttrs(): Iterator<XmlAttr> {
        return attrs.iterator()
    }

    fun getItems(): Iterator<XmlNode> {
        return items.iterator()
    }

    fun hasAttr(name: String): Boolean {
        return getAttr(name).isPresent
    }

    fun getAttr(name: String): Optional<XmlAttr> {
        return attrs.stream()
            .filter { p: XmlAttr -> p.getName() == name }
            .findFirst()
    }

    fun remove(node: XmlNode) {
        items.remove(node)
    }

    fun writeAsString(stream: OutputStream) {
        val docBuilder: DocumentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val doc = docBuilder.newDocument()
        val element = doc.createElement(name)
        visit(element)
        val transformer: Transformer = TransformerFactory.newInstance().newTransformer()
        // pretty print
        transformer.setOutputProperty(OutputKeys.INDENT, "yes")
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
        transformer.transform(DOMSource(doc), StreamResult(stream))
    }

    private fun visit(element: Element) {
        val ownerDocument = element.ownerDocument
        attrs.forEach { attr: XmlAttr -> element.setAttribute(attr.getName(), attr.getValue()) }
        items.forEach { node: XmlNode ->
            val childElement = ownerDocument.createElement(node.getName())
            element.appendChild(childElement)
            node.visit(childElement)
        }
    }
}
