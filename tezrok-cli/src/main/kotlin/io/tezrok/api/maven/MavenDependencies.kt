package io.tezrok.api.maven

import java.util.stream.Stream

/**
 * Interface for accessing maven dependencies
 */
interface MavenDependencies {
    fun getDependencies(): Stream<MavenDependency>

    fun getDependency(groupId: String, artifactId: String): MavenDependency?

    fun addDependency(dependency: MavenDependency): Boolean

    fun addDependency(dependency: String): Boolean = addDependency(MavenDependency.of(dependency))

    fun removeDependencies(dependencies: List<MavenDependency>): Boolean
}
