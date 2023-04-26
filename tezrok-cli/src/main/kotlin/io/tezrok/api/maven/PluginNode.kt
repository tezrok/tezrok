package io.tezrok.api.maven

import io.tezrok.api.xml.XmlNode
import java.util.stream.Stream

class PluginNode(val node: XmlNode) : MavenDependencies {
    val dependency: MavenDependency = node.toDependency()

    val executions: Stream<ExecutionNode> = node.nodesByPath("executions/execution")
        .map { ExecutionNode(it) }

    fun addExecution(id: String, phase: BuildPhase, goal: String): ExecutionNode =
        ExecutionNode(
            node.getOrCreate("executions")
                .getOrCreate("execution")
                .getOrCreate("id").setValue(id).and()
                .getOrCreate("goals").getOrCreate("goal").setValue(goal).and().and()
                .getOrCreate("phase").setValue(phase.id).and()
        )

    fun getConfiguration(): ConfigurationNode =
        ConfigurationNode(node.getOrCreate("configuration"))

    override fun getDependencies(): Stream<MavenDependency> = dependenciesAccess().getDependencies()

    override fun getDependency(groupId: String, artifactId: String): MavenDependency? =
        dependenciesAccess().getDependency(groupId, artifactId)

    override fun addDependency(dependency: MavenDependency): Boolean =
        dependenciesAccess().addDependency(dependency)

    override fun removeDependencies(dependencies: List<MavenDependency>): Boolean =
        dependenciesAccess().removeDependencies(dependencies)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PluginNode

        return node == other.node
    }

    override fun hashCode(): Int = node.hashCode()

    private fun dependenciesAccess() = MavenDependenciesAccess(node)
}

class ExecutionNode(val node: XmlNode) {
    val id: String = node.getNodeValue("id")
    val phase: BuildPhase = BuildPhase.fromId(node.getNodeValue("phase"))
    val goal: String = node.getNodeValue("goal")

    fun getConfiguration(): ConfigurationNode = ConfigurationNode(node.getOrCreate("configuration"))

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ExecutionNode

        return node == other.node
    }

    override fun hashCode(): Int = node.hashCode()
}

class ConfigurationNode(val node: XmlNode) {
    val id: String = node.getNodeValue("id")
    val phase: BuildPhase = BuildPhase.fromId(node.getNodeValue("phase"))
    val goal: String = node.getNodeValue("goal")

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ConfigurationNode

        return node == other.node
    }

    override fun hashCode(): Int = node.hashCode()
}
