package io.tezrok.api.maven

data class MavenDependency(val groupId: String, val artifactId: String, val version: String, val scope: String = "") {
    fun shortId(): String = "$groupId:$artifactId"

    fun fullId(): String = "$groupId:$artifactId:$version:$scope"

    fun withGroupId(groupId: String): MavenDependency = MavenDependency(groupId, artifactId, version)

    fun withArtifactId(artifactId: String): MavenDependency = MavenDependency(groupId, artifactId, version)

    fun withVersion(version: String): MavenDependency = MavenDependency(groupId, artifactId, version)

    fun withScope(scope: String): MavenDependency = MavenDependency(groupId, artifactId, version, scope)

    companion object {
        /**
         * Parse maven dependency from string
         *
         * Note: version and scope are optional
         *
         * @param dependency dependency in format "groupId:artifactId:version:scope"
         */
        @JvmStatic
        fun of(dependency: String): MavenDependency {
            val parts = dependency.split(":")
            return MavenDependency(
                    groupId = parts[0],
                    artifactId = parts[1],
                    version = if (parts.size > 2) parts[2] else "",
                    scope = if (parts.size > 3) parts[3] else ""
            )
        }
    }
}
