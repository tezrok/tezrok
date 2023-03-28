package io.tezrok.api

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.time.Clock

/**
 * Used to provide context for any [TezrokGenerator] or [TezrokFeature]
 */
interface GeneratorContext {
    fun isGenerateTime(): Boolean = true

    fun getCharset(): Charset = StandardCharsets.UTF_8

    fun getAuthor(): String

    fun getClock(): Clock = Clock.systemDefaultZone()
}
