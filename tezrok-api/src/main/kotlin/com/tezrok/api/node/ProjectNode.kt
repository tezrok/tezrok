package com.tezrok.api.node

interface ProjectNode : Node {
    fun getModules(): ModuleNodes

    fun getSettings(): ProjectSettingsNode
}
