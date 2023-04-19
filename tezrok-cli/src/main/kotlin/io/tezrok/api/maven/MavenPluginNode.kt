package io.tezrok.api.maven

import io.tezrok.api.xml.XmlNode

class MavenPluginNode(val node: XmlNode) {
    val dependency: MavenDependency = node.toDependency()
}
