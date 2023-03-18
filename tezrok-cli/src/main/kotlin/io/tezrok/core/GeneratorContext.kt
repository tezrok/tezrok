package io.tezrok.core

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

class GeneratorContext {
    fun isGenerateTime(): Boolean = true
    fun getCharset(): Charset = StandardCharsets.UTF_8
    fun getAuthor(): String = "TezrokUser"
}
