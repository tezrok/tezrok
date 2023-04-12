package io.tezrok.api.xml

import io.tezrok.api.node.FileNode
import io.tezrok.api.node.Node
import io.tezrok.util.ByteArrayUtil
import java.io.InputStream
import java.io.OutputStream

open class XmlFileNode(name: String, rootName: String, parent: Node? = null) : FileNode(name, parent) {
    private val xml: XmlNode = XmlNode(rootName)

    open fun getXml(): XmlNode = xml

    override fun isEmpty(): Boolean = false

    /**
     * Please, note that method calculate size of xml by converting it to string each time.
     */
    @Synchronized
    override fun getSize(): Long = getBytes().size.toLong()

    override fun getOutputStream(): OutputStream {
        // TODO: implement
        throw UnsupportedOperationException("Xml file can be edited only via object model")
    }

    @Synchronized
    override fun getInputStream(): InputStream = ByteArrayUtil.outputAsInput(xml::writeAsString)

    override fun toString(): String = xml.toString()

    protected fun getBytes() = ByteArrayUtil.outputAsArray(xml::writeAsString)
}
