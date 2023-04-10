package io.tezrok.util

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream

object ByteArrayUtil {
    /**
     * Create an [OutputStream] which efficiently converts to [InputStream]
     */
    fun outputAsInput(block: (OutputStream) -> Unit): InputStream = ByteArrayInputStream(outputAsArray(block))

    /**
     * Create an [OutputStream] and gets internal [ByteArray]
     */
    fun outputAsArray(block: (OutputStream) -> Unit): ByteArray {
        val stream = ByteArrayOutputStreamInternal()
        block(stream)
        return stream.getByteArray()
    }

    private class ByteArrayOutputStreamInternal : ByteArrayOutputStream() {
        fun getByteArray(): ByteArray {
            return buf
        }
    }
}
