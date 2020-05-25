package io.tezrok.api

/**
 * Mark class related for some feature.
 */
@kotlin.annotation.Target
@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
annotation class Feature(
        /**
         * Name of the feature.
         */
        val value: String)
