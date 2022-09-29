package com.tencent.bkrepo.common.api.util

import java.io.InputStream
import java.nio.charset.Charset

/**
 * 流工具
 * */
object StreamUtils {

    /**
     * 阻塞读取满data
     * */
    fun readFully(inputStream: InputStream, data: ByteArray): Int {
        var pos = 0
        val total = data.size
        var remain = total - pos
        do {
            val bytes = inputStream.read(data, pos, remain)
            if (bytes == -1) {
                return pos
            }
            pos += bytes
            remain = total - pos
        } while (remain > 0)
        return pos
    }

    fun InputStream.readText(charset: Charset = Charsets.UTF_8) = this.use {
        it.reader(charset).use { reader -> reader.readText() }
    }
}
