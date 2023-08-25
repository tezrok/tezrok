package io.tezrok.api.maven

data class MavenDependency(val groupId: String, val artifactId: String, val version: String, val scope: String = "", val packaging: String = "") {
    init {
        check(scope.isEmpty() || supportedScopes.contains(scope)) { "Unsupported scope: $scope" }
        check(packaging.isEmpty() || supportedPackages.contains(packaging)) { "Unsupported packaging: $packaging" }
    }

    fun shortId(): String = "$groupId:$artifactId"

    fun fullId(): String = "$groupId:$artifactId:$version:$scope"

    fun withGroupId(groupId: String): MavenDependency = MavenDependency(groupId, artifactId, version, scope, packaging)

    fun withArtifactId(artifactId: String): MavenDependency = MavenDependency(groupId, artifactId, version, scope, packaging)

    fun withVersion(version: String): MavenDependency = MavenDependency(groupId, artifactId, version, scope, packaging)

    fun withScope(scope: String): MavenDependency = MavenDependency(groupId, artifactId, version, scope, packaging)

    fun withPackaging(packaging: String): MavenDependency = MavenDependency(groupId, artifactId, version, scope, packaging)

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
                    scope = if (parts.size > 3) parts[3] else "",
                    packaging = if (parts.size > 4) parts[4] else ""
            )
        }

        private val supportedScopes = setOf("compile", "provided", "runtime", "test", "system")
        private val supportedPackages = setOf("jar", "war", "pom")
    }
}
