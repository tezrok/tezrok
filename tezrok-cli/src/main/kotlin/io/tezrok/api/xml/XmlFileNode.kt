package io.tezrok.api.xml

import io.tezrok.api.node.BaseNode
import io.tezrok.api.node.FileNode
import io.tezrok.util.ByteArrayUtil
import java.io.InputStream
import java.io.OutputStream

open class XmlFileNode(name: String, parent: BaseNode? = null) : FileNode(name, parent) {
    private val xml: XmlNode = XmlNode(name)

    fun getXmlNode(): XmlNode = xml

    override fun isEmpty(): Boolean = false

    /**
     * Please, note that method calculate size of xml by converting it to string each time.
     */
    @Synchronized
    override fun getSize(): Long = ByteArrayUtil.outputAsArray(xml::writeAsString).size.toLong()

    override fun getOutputStream(): OutputStream {
        throw UnsupportedOperationException("Xml file can be edited only via object model")
    }

    @Synchronized
    override fun getInputStream(): InputStream = ByteArrayUtil.outputAsInput(xml::writeAsString)
}
