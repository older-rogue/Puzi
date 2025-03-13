package com.zx.puzi.utils

import android.util.Base64

object DecodeUtils {
    fun showdown(string: String, key: String): String {
        val downloadUrl = de(string, key)
        val components = downloadUrl.split("|")
        return components[0]
    }

    private fun de(string: String, key: String): String {
        val decodedString = strDecode(string)
        val len = key.length
        val code = StringBuilder()

        for (i in decodedString.indices) {
            val k = i % len
            code.append((decodedString[i].code xor key[k].code).toChar())
        }

        return strDecode(code.toString())
    }

    private fun strDecode(str: String): String {
        return utf8To16(base64Decode(str))
    }

    private fun utf8To16(str: String): String {
        val out = StringBuilder()
        var i = 0
        while (i < str.length) {
            val c = str[i].code
            when (c shr 4) {
                in 0 .. 7 -> out.append(str[i])
                12, 13 -> {
                    val char2 = str[++i].code
                    out.append(((c and 0x1F) shl 6 or (char2 and 0x3F)).toChar())
                }

                14 -> {
                    val char2 = str[++i].code
                    val char3 = str[++i].code
                    out.append(((c and 0x0F) shl 12 or ((char2 and 0x3F) shl 6) or (char3 and 0x3F)).toChar())
                }
            }
            i++
        }
        return out.toString()
    }

    private fun base64Decode(str: String): String {
        return String(Base64.decode(str, Base64.NO_WRAP), Charsets.UTF_8)
    }
}






