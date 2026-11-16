package io.github.realkhfan.onda.core

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Everything between your phone and your PC is encrypted (AES-256-GCM), so others on your Wi-Fi can't read or
 * change it. Pairing uses the 6-digit code the PC shows; after that a random key only the two devices know.
 */
object LinkCrypto {
    private val random = SecureRandom()

    /** The key for pairing, from the code on the PC's screen (slow on purpose, against guessing). */
    fun pairKey(code: String): ByteArray {
        val spec = PBEKeySpec(code.toCharArray(), "onda-pair-v1".toByteArray(), 100_000, 256)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    }

    /** The key after pairing; the token is 32 random bytes, so a hash is enough. */
    fun linkKey(token: String): ByteArray = MessageDigest.getInstance("SHA-256").digest("onda-link-v1|$token".toByteArray())

    fun newToken(): String = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(32).also(random::nextBytes))

    /** A random 6-digit code. */
    fun newCode(): String = "%06d".format(random.nextInt(1_000_000))

    /** [aad]: what the message is for (its path), so a message can't be reused for another request. */
    fun seal(key: ByteArray, plain: ByteArray, aad: String): ByteArray {
        val nonce = ByteArray(12).also(random::nextBytes)
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
        c.updateAAD(aad.toByteArray())
        return nonce + c.doFinal(plain)
    }

    /** Null when it wasn't sealed with this key (or was changed on the way). */
    fun open(key: ByteArray, sealed: ByteArray, aad: String): ByteArray? = runCatching {
        if (sealed.size < 28) return null
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, sealed, 0, 12))
        c.updateAAD(aad.toByteArray())
        c.doFinal(sealed, 12, sealed.size - 12)
    }.getOrNull()

    /** Messages older or newer than this (by the clock in them) are refused, so a recorded one can't be replayed later. */
    const val MAX_SKEW_MS = 10 * 60_000L

    /** The port the PC listens on (TCP for requests, UDP to be found). */
    const val PORT = 45231
}
