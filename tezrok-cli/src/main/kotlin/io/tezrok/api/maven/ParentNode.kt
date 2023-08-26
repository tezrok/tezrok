package io.tezrok.api.maven

import io.tezrok.api.xml.XmlNode

/**
 * Part of the pom.xml file that contains information about the parent project
 */
open class ParentNode(val node: XmlNode) {
    var dependencyId: MavenDependency
        get() = node.toDependency()
        set(value) = node.setDependency(value)
}
