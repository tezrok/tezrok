package io.tezrok.api.maven

import io.tezrok.api.xml.XmlNode

/**
 * Part of the pom.xml file that contains information about the parent project
 */
open class ParentNode(val node: XmlNode) {
    var dependencyId: MavenDependency
        get() = getDependencyIdInternal()
        set(value) = setDependencyIdInternal(value)

    private fun setDependencyIdInternal(value: MavenDependency) {
        if (value.groupId.isNotBlank())
            node.getOrCreate(PomNode.GROUP_ID, value.groupId)
        if (value.artifactId.isNotBlank())
            node.getOrCreate(PomNode.ARTIFACT_ID, value.artifactId)
        if (value.version.isNotBlank())
            node.getOrCreate(PomNode.VERSION, value.version)
    }

    private fun getDependencyIdInternal(): MavenDependency = node.let { xml ->
        MavenDependency(
            xml.getNodeValue(PomNode.GROUP_ID),
            xml.getNodeValue(PomNode.ARTIFACT_ID),
            xml.getNodeValue(PomNode.VERSION)
        )
    }
}
