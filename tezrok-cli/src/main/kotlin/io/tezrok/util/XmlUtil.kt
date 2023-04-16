package io.tezrok.util

import org.w3c.dom.NamedNodeMap
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.util.stream.Stream
import java.util.stream.StreamSupport

object XmlUtil {
    fun nodeStream(nodeMap: NamedNodeMap?): Stream<Node> {
        val map = nodeMap ?: return Stream.empty()

        val iterator = object : Iterator<Node> {
            private var index = 0
            override fun hasNext(): Boolean = index < map.length
            override fun next(): Node {
                val node = map.item(index)
                index++
                return node
            }
        }

        return StreamSupport.stream(Iterable { iterator }.spliterator(), false)
    }

    fun nodeStream(node: NodeList?): Stream<Node> {
        val list = node ?: return Stream.empty()

        val iterator = object : Iterator<Node> {
            private var index = 0
            override fun hasNext(): Boolean = index < list.length
            override fun next(): Node {
                val node = list.item(index)
                index++
                return node
            }
        }

        return StreamSupport.stream(Iterable { iterator }.spliterator(), false)
    }
}
