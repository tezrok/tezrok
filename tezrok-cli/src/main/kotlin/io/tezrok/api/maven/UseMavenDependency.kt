package io.tezrok.api.maven

/**
 * A declarative method of adding [MavenDependency] into current module
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class UseMavenDependency(val value: String)
