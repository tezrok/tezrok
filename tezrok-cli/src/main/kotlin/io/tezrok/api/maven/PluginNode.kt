package io.tezrok.api.maven

import io.tezrok.api.xml.XmlNode
import java.util.stream.Stream

class PluginNode(val node: XmlNode) : MavenDependencies by MavenDependenciesAccess(node) {
    val dependency: MavenDependency = node.toDependency()

    val executions: Stream<ExecutionNode> = node.nodesByPath("executions/execution")
        .map { ExecutionNode(it) }

    fun addExecution(id: String, phase: BuildPhase, goal: String): ExecutionNode =
        ExecutionNode(
            node.getOrAdd("executions")
                .add("execution")
                .add("id").setValue(id).and()
                .add("goals").add("goal").setValue(goal).and().and()
                .add("phase").setValue(phase.id).and()
        )

    fun getConfiguration(): ConfigurationNode = ConfigurationNode(node.getOrAdd("configuration"))

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PluginNode

        return node == other.node
    }

    override fun hashCode(): Int = node.hashCode()
}

class ExecutionNode(val node: XmlNode) {
    val id: String = node.getNodeValue("id")
    val phase: BuildPhase = BuildPhase.fromId(node.getNodeValue("phase"))
    val goal: String = node.getNodeValue("goal")

    fun getConfiguration(): ConfigurationNode = ConfigurationNode(node.getOrAdd("configuration"))

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ExecutionNode

        return node == other.node
    }

    override fun hashCode(): Int = node.hashCode()
}

class ConfigurationNode(val node: XmlNode) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ConfigurationNode

        return node == other.node
    }

    override fun hashCode(): Int = node.hashCode()
}
