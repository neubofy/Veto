package com.neubofy.veto.utils

import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom

object CypherUtils {
    const val MIN_PASSWORD_LENGTH = 8

    private const val CONTEXT_STRING_Veto_PIN = "context:vetoPin"
    private const val SALT_LENGTH = 16

    @JvmStatic
    fun hashPasswordForVetoPin(password: String): String {
        val saltedPassword = CONTEXT_STRING_Veto_PIN + password
        val salt = generateSecureRandom(SALT_LENGTH)
        val hash = hashWithSha256(saltedPassword, salt)
        
        // Format: saltBase64$hashBase64
        val saltBase64 = encodeBase64(salt)
        val hashBase64 = encodeBase64(hash)
        return "$saltBase64$$hashBase64"
    }

    @JvmStatic
    fun checkPasswordForVetoPin(expectedHashString: String, password: String): Boolean {
        if (expectedHashString.isEmpty() || password.isEmpty() || !expectedHashString.contains("$")) {
            return false
        }

        val parts = expectedHashString.split("$")
        if (parts.size != 2) return false

        val salt = try {
            decodeBase64(parts[0])
        } catch (e: Exception) {
            return false
        }
        val expectedHash = try {
            decodeBase64(parts[1])
        } catch (e: Exception) {
            return false
        }

        val saltedPassword = CONTEXT_STRING_Veto_PIN + password
        val actualHash = hashWithSha256(saltedPassword, salt)

        return MessageDigest.isEqual(expectedHash, actualHash)
    }

    private fun hashWithSha256(password: String, salt: ByteArray): ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt)
        return md.digest(password.toByteArray(StandardCharsets.UTF_8))
    }

    @JvmStatic
    fun encodeBase64(toEncode: ByteArray): String {
        return Base64.encodeToString(toEncode, Base64.NO_WRAP)
    }

    @JvmStatic
    fun decodeBase64(toDecode: String): ByteArray {
        return Base64.decode(toDecode, Base64.NO_WRAP)
    }

    @JvmStatic
    fun generateSecureRandom(lengthInBytes: Int): ByteArray {
        val random = SecureRandom()
        val randomBytes = ByteArray(lengthInBytes)
        random.nextBytes(randomBytes)
        return randomBytes
    }
}
