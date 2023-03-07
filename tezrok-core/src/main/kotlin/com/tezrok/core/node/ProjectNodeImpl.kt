package com.tezrok.core.node

import com.tezrok.api.node.*
import java.util.stream.Stream

internal class ProjectNodeImpl(nodeId: NodeId, nodeProperties: NodeProperties, nodeSupport: NodeSupport) :
    AbstractNode(nodeId, null, nodeProperties, nodeSupport), ProjectNode {
    override fun getModules(): ModuleNodes {
        TODO()
    }

    override fun getSettings(): ProjectSettingsNode {
        TODO()
    }

    override fun getChildren(): Stream<Node> = Stream.concat(Stream.of(getModules(), getSettings()), getOtherChildren())

    override fun clone(): Node {
        TODO("Not yet implemented")
    }

    override fun <T : Node> asChild(clazz: Class<T>): T? = when (clazz) {
        ModuleNodes::class.java -> getModules() as T
        ProjectSettingsNode::class.java -> getSettings() as T
        else -> getOtherChildren().filter { clazz.isInstance(it) }.findFirst().orElse(null) as T
    }
}
