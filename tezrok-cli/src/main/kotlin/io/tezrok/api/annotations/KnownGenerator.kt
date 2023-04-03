package io.tezrok.api.annotations

/**
 * An annotation that marks a generator as known to the internal generator provider
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class KnownGenerator()
