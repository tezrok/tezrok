package io.tezrok.api.model.maven

data class Pom(var version: Version,
               var type: String,
               val properties: MutableList<Property>,
               val dependencies: MutableList<Dependency>)

data class Version(val groupId: String,
                   val artifactId: String,
                   val version: String)

data class Property(val name: String,
                    val value: String)

data class Dependency(val groupId: String,
                      val artifactId: String,
                      val version: String,
                      val scope: String)
