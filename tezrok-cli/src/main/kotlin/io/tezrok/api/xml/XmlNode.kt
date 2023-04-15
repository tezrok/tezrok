package io.tezrok.api.xml

import io.tezrok.util.ByteArrayUtil
import net.jcip.annotations.NotThreadSafe
import org.apache.commons.lang3.Validate
import org.w3c.dom.Element
import java.io.OutputStream
import java.util.*
import java.util.stream.Stream
import java.util.stream.StreamSupport
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

/**
 * Class works with xml
 */
@NotThreadSafe
open class XmlNode private constructor(private val element: Element, private val parent: XmlNode? = null) {
    fun getName(): String = element.tagName

    fun getValue(): String? = element.nodeValue

    fun setName(name: String): XmlNode {
        element.ownerDocument.renameNode(element, element.namespaceURI, name)
        return this
    }

    fun setValue(value: String?): XmlNode {
        element.nodeValue = value
        return this
    }

    fun getParent(): XmlNode? = parent

    fun isRoot(): Boolean = parent == null

    fun and(): XmlNode = getParent() ?: throw IllegalStateException("No parent node")

    /**
     * Creates a new node or returns first existing one
     */
    fun getOrCreate(name: String): XmlNode =
        itemStream().filter { p: XmlNode -> p.getName() == name }
            .findFirst()
            .orElseGet { add(name) }

    fun add(name: String): XmlNode {
        val childElem = element.ownerDocument.createElement(name)
        return XmlNode(childElem, parent = this)
    }

    fun get(name: String): List<XmlNode> = itemStream().filter { p: XmlNode -> p.getName() == name }.toList()

    fun addAttr(name: String, value: String): XmlNode {
        element.setAttribute(name, value)
        return this
    }

    fun getAttr(name: String): Optional<XmlAttr> = Optional.of(name)
        .filter(element::hasAttribute)
        .map(element::getAttribute)
        .map { XmlAttr(name, it) }

    fun hasAttr(name: String): Boolean = element.hasAttribute(name)

    fun removeAttr(name: String): Boolean = element.removeAttribute(name)

    val isEmpty: Boolean = element.childNodes.length == 0

    fun getAttrs(): List<XmlAttr> {
        return attrs.map { XmlAttr(it.key, it.value) }
    }

    fun getItems(): List<XmlNode> = itemStream().toList()

    protected fun itemStream(): Stream<XmlNode> {
        val nodes = element.childNodes
        val iterator = object : Iterator<XmlNode> {
            private var index = 0
            override fun hasNext(): Boolean = index < nodes.length
            override fun next(): XmlNode {
                val node = nodes.item(index)
                index++
                return XmlNode(node as Element, parent = this@XmlNode)
            }
        }

        return StreamSupport.stream(Iterable { iterator }.spliterator(), false)
    }

    fun remove(nodes: List<XmlNode>): Boolean = nodes.map { it.element }
        .filter { it.parentNode == this }
        .map(element::removeChild)
        .isNotEmpty()

    fun writeAsString(stream: OutputStream) {
        val transformer: Transformer = TransformerFactory.newInstance().newTransformer()
        // pretty print
        transformer.setOutputProperty(OutputKeys.INDENT, "yes")
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
        transformer.transform(DOMSource(element.ownerDocument), StreamResult(stream))
    }

    override fun toString(): String = ByteArrayUtil.outputAsArray(this::writeAsString).toString(Charsets.UTF_8)

    companion object {
        fun newNode(name: String): XmlNode {
            Validate.notBlank(name, "Xml name cannot be blank")
            val docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            val doc = docBuilder.newDocument()
            val element = doc.createElement(name)
            return XmlNode(element)
        }
    }
}
