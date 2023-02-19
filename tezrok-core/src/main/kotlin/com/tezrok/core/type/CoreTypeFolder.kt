package com.tezrok.core.type

import com.tezrok.api.error.TezrokException
import com.tezrok.api.node.Node
import com.tezrok.api.node.Nodeable
import com.tezrok.api.type.TypeDef
import com.tezrok.api.type.TypeFolder

/**
 * Implementation of [TypeFolder]
 */
internal class CoreTypeFolder : TypeFolder, Nodeable {
    private val types: MutableList<TypeDef> = mutableListOf()

    @Synchronized
    override fun getTypes(): List<TypeDef> = types.toList()

    @Synchronized
    override fun addType(name: String): TypeDef {
        types.find { it.getName() == name }
            ?.let { throw TezrokException("Type with name $name already exists") }

        val typeDef = CoreTypeDef(name)
        types.add(typeDef)
        return typeDef
    }

    @Synchronized
    override fun removeType(type: TypeDef): Boolean {
        return types.remove(type)
    }

    override fun asNode(): Node {
        TODO("Not yet implemented")
    }
}
