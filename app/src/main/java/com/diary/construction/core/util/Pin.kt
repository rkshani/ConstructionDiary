package com.diary.construction.core.util

import java.security.MessageDigest
import java.security.SecureRandom

/** The PIN is never stored. We keep "salt:sha256(salt + pin)". */
object Pin {
    const val LENGTH = 4

    fun hash(pin: String): String {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }.toHex()
        return "$salt:${digest(salt, pin)}"
    }

    fun matches(pin: String, stored: String?): Boolean {
        if (stored.isNullOrBlank()) return false
        val parts = stored.split(":")
        if (parts.size != 2) return false
        return MessageDigest.isEqual(digest(parts[0], pin).toByteArray(), parts[1].toByteArray())
    }

    private fun digest(salt: String, pin: String): String =
        MessageDigest.getInstance("SHA-256").digest((salt + pin).toByteArray(Charsets.UTF_8)).toHex()

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
}
