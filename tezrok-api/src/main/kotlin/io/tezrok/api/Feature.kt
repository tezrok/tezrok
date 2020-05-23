package io.tezrok.api

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

/**
 * Mark class related for some feature.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
annotation class Feature(
        /**
         * Name of the feature.
         */
        val value: String)
