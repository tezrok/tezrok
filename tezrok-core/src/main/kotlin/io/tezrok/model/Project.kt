package io.tezrok.model

/**
 * Properties of project
 */
class Project(name: String,
              description: String? = null,
              val modules: List<Module>? = null) : BaseEntity(name, description, "Project")




