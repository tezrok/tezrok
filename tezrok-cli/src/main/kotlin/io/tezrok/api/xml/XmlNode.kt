package io.tezrok.api.xml

import io.tezrok.util.ByteArrayUtil
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

open class XmlNode(private var name: String, private var value: String? = null, private val parent: XmlNode? = null) {
    private val attrs: MutableMap<String, String> = HashMap()
    private val items: MutableList<XmlNode> = ArrayList()

    init {
        Validate.notBlank(name, "Xml node name cannot be blank")
    }

    fun getName(): String = name

    fun getValue(): String? = value

    fun setName(name: String): XmlNode {
        this.name = name
        return this
    }

    fun setValue(value: String?): XmlNode {
        this.value = value
        return this
    }

    fun getParent(): XmlNode? = parent

    fun isRoot(): Boolean = parent == null

    fun and(): XmlNode = getParent() ?: throw IllegalStateException("No parent node")

    /**
     * Creates a new node or returns first existing one
     */
    @Synchronized
    fun getOrCreate(name: String): XmlNode = items.find { p: XmlNode -> p.getName() == name } ?: add(name)

    @Synchronized
    fun add(name: String): XmlNode {
        val child = XmlNode(name, parent = this)
        items.add(child)
        return child
    }

    @Synchronized
    fun get(name: String): List<XmlNode> = items.filter { it.getName() == name }

    @Synchronized
    fun addAttr(name: String, value: String): XmlNode {
        attrs[name] = value
        return this
    }

    @Synchronized
    fun getAttr(name: String): Optional<XmlAttr> {
        return Optional.ofNullable(attrs[name]).map { XmlAttr(name, it) }
    }

    @Synchronized
    fun hasAttr(name: String): Boolean = attrs.containsKey(name)

    @Synchronized
    fun removeAttr(name: String): Boolean = attrs.remove(name) != null

    val isEmpty: Boolean = items.isEmpty()

    @Synchronized
    fun getAttrs(): List<XmlAttr> {
        return attrs.map { XmlAttr(it.key, it.value) }
    }

    @Synchronized
    fun getItems(): List<XmlNode> {
        return items.toList()
    }

    @Synchronized
    fun remove(node: XmlNode): Boolean = items.remove(node)

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
        attrs.forEach { (key, value) -> element.setAttribute(key, value) }
        items.forEach { node: XmlNode ->
            val childElement = ownerDocument.createElement(node.getName())
            element.appendChild(childElement)
            node.visit(childElement)
        }
    }

    override fun toString(): String = ByteArrayUtil.outputAsArray(this::writeAsString).toString(Charsets.UTF_8)
}
