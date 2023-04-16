package io.tezrok.api.xml

import io.tezrok.util.ByteArrayUtil
import io.tezrok.util.XmlUtil
import net.jcip.annotations.NotThreadSafe
import org.apache.commons.lang3.Validate
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.io.OutputStream
import java.util.*
import java.util.stream.Stream
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import javax.xml.xpath.XPath
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory


/**
 * Class works with xml
 */
@NotThreadSafe
open class XmlNode private constructor(private val element: Element) {
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

    fun getParent(): XmlNode? = element.parentNode?.let { XmlNode(it as Element) }

    fun isRoot(): Boolean = element.parentNode == null

    fun and(): XmlNode = getParent() ?: throw IllegalStateException("No parent node")


    fun add(name: String): XmlNode {
        val childElem = element.ownerDocument.createElement(name)
        return XmlNode(childElem)
    }

    fun get(name: String): XmlNode? = itemsStream()
        .filter { it.getName() == name }
        .findFirst()
        .orElse(null)

    /**
     * Creates a new node or returns first existing one
     */
    fun getOrCreate(name: String): XmlNode = itemsStream()
        .filter { it.getName() == name }
        .findFirst()
        .orElseGet { add(name) }

    fun getAll(name: String): List<XmlNode> = itemsStream().filter { it.getName() == name }.toList()

    fun getNodeValue(name: String, defValue: String = ""): String = get(name)?.getValue() ?: defValue

    fun addAttr(name: String, value: String): XmlNode {
        element.setAttribute(name, value)
        return this
    }

    fun getAttr(name: String): Optional<XmlAttr> = Optional.of(name)
        .filter(element::hasAttribute)
        .map(element::getAttribute)
        .map { XmlAttr(name, it) }

    fun hasAttr(name: String): Boolean = element.hasAttribute(name)

    fun removeAttr(name: String) = element.removeAttribute(name)

    val isEmpty: Boolean = element.childNodes.length == 0

    fun getAttrs(): List<XmlAttr> = XmlUtil.nodeStream(element.attributes)
        .map { XmlAttr(it.nodeName, it.nodeValue) }
        .toList()

    fun getItems(): Stream<XmlNode> = itemsStream()

    fun remove(nodes: List<XmlNode>): Boolean = nodes.map { it.element }
        .filter { it.parentNode == this }
        .toList()
        .map(element::removeChild)
        .isNotEmpty()

    fun nodesByPath(expression: String): Stream<XmlNode> {
        val nodeList = xPath.value.compile(expression).evaluate(element, XPathConstants.NODESET) as NodeList

        return XmlUtil.nodeStream(nodeList).map { XmlNode(it as Element) }
    }

    fun writeAsString(stream: OutputStream) {
        val transformer: Transformer = TransformerFactory.newInstance().newTransformer()
        // pretty print
        transformer.setOutputProperty(OutputKeys.INDENT, "yes")
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
        transformer.transform(DOMSource(element.ownerDocument), StreamResult(stream))
    }

    override fun toString(): String = ByteArrayUtil.outputAsArray(this::writeAsString).toString(Charsets.UTF_8)

    private fun itemsStream(): Stream<XmlNode> = XmlUtil.nodeStream(element.childNodes)
        .filter { it.nodeType == Node.ELEMENT_NODE }
        .map { XmlNode(it as Element) }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as XmlNode

        return element == other.element
    }

    override fun hashCode(): Int {
        return element.hashCode()
    }

    companion object {
        private val xPath: Lazy<XPath> = lazy { XPathFactory.newInstance().newXPath() }

        fun newNode(name: String): XmlNode {
            Validate.notBlank(name, "Xml name cannot be blank")
            val docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            val doc = docBuilder.newDocument()
            val element = doc.createElement(name)
            return XmlNode(element)
        }
    }
}
