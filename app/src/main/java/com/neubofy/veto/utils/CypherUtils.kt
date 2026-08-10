package com.neubofy.veto.utils

import android.util.Base64
import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import org.bouncycastle.util.Arrays
import org.bouncycastle.util.io.pem.PemObject
import org.bouncycastle.util.io.pem.PemWriter
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.StringWriter
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.Signature
import java.security.interfaces.RSAPrivateCrtKey
import java.security.interfaces.RSAPrivateKey
import java.security.spec.EncodedKeySpec
import java.security.spec.MGF1ParameterSpec
import javax.crypto.spec.OAEPParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.PSSParameterSpec
import java.security.spec.RSAPublicKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PSource
import javax.crypto.spec.SecretKeySpec

object CypherUtils {
    const val MIN_PASSWORD_LENGTH = 8

    private const val AES_GCM_IV_SIZE_BYTES = 12
    private const val AES_GCM_KEY_SIZE_BYTES = 32
    private const val AES_GCM_TAG_SIZE_BITS = 128
    private const val RSA_KEY_SIZE_BITS = 3072

    private const val ARGON2_T = 1
    private const val ARGON2_P = 4
    private const val ARGON2_M = 32768
    private const val ARGON2_HASH_LENGTH = 32
    private const val ARGON2_SALT_LENGTH = 16

    private const val CONTEXT_STRING_ASYM_KEY_WRAP = "context:asymmetricKeyWrap"
    private const val CONTEXT_STRING_Veto_PIN = "context:vetoPin"
    private const val CONTEXT_STRING_LOGIN = "context:loginAuthentication"

    private const val CONTEXT_PREFIX = "context:"

    class Argon2Result(val hash: ByteArray, val params: Argon2Parameters)

    @JvmStatic
    fun hashPasswordForVetoPin(password: String): String {
        val saltedPassword = CONTEXT_STRING_Veto_PIN + password
        val salt = generateSecureRandom(ARGON2_SALT_LENGTH)
        val result = hashPasswordArgon2(saltedPassword, salt)
        return Argon2EncodingUtils.encode(result.hash, result.params)
    }


    @JvmStatic
    fun hashPasswordForLogin(password: String): String {
        val salt = generateSecureRandom(ARGON2_SALT_LENGTH)
        return hashPasswordForLogin(password, salt)
    }

    @JvmStatic
    fun hashPasswordForLogin(password: String, saltBase64: String): String {
        val salt = Base64.decode(saltBase64, Argon2EncodingUtils.BASE64_FLAGS)
        return hashPasswordForLogin(password, salt)
    }

    @JvmStatic
    fun hashPasswordForLogin(password: String, saltBytes: ByteArray): String {
        val saltedPassword = CONTEXT_STRING_LOGIN + password
        val result = hashPasswordArgon2(saltedPassword, saltBytes)
        return Argon2EncodingUtils.encode(result.hash, result.params)
    }

    @JvmStatic
    fun hashPasswordForKeyWrap(password: String): Argon2Result {
        val salt = generateSecureRandom(ARGON2_SALT_LENGTH)
        return hashPasswordForKeyWrap(password, salt)
    }

    @JvmStatic
    fun hashPasswordForKeyWrap(password: String, saltBytes: ByteArray): Argon2Result {
        val saltedPassword = CONTEXT_STRING_ASYM_KEY_WRAP + password
        return hashPasswordArgon2(saltedPassword, saltBytes)
    }

    private fun hashPasswordArgon2(password: String, salt: ByteArray): Argon2Result {
        if (!password.startsWith(CONTEXT_PREFIX)) {
            throw RuntimeException("Missing context string")
        }
        val passwordBytes = password.toByteArray(StandardCharsets.UTF_8)
        val out = ByteArray(ARGON2_HASH_LENGTH)

        val params = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withVersion(Argon2Parameters.ARGON2_VERSION_13)
            .withIterations(ARGON2_T)
            .withParallelism(ARGON2_P)
            .withMemoryAsKB(ARGON2_M)
            .withSalt(salt)
            .build()

        val generator = Argon2BytesGenerator()
        generator.init(params)
        generator.generateBytes(passwordBytes, out)

        return Argon2Result(out, params)
    }

    @JvmStatic
    fun checkPasswordForVetoPin(expectedHash: String, password: String): Boolean {
        return checkPassword(expectedHash, CONTEXT_STRING_Veto_PIN + password)
    }


    @JvmStatic
    fun checkPasswordForLogin(expectedHash: String, password: String): Boolean {
        return checkPassword(expectedHash, CONTEXT_STRING_LOGIN + password)
    }

    private fun checkPassword(expectedHash: String, password: String): Boolean {
        if (expectedHash.isEmpty() || password.isEmpty()) {
            return false
        }

        val decodedExpected = try {
            Argon2EncodingUtils.decode(expectedHash)
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
            return false
        }

        val passwordBytes = password.toByteArray(StandardCharsets.UTF_8)
        val actualBytes = ByteArray(decodedExpected.hash.size)

        val generator = Argon2BytesGenerator()
        generator.init(decodedExpected.parameters)
        generator.generateBytes(passwordBytes, actualBytes)

        return Arrays.constantTimeAreEqual(decodedExpected.hash, actualBytes)
    }

    @JvmStatic
    fun genRsaKeyPair(): KeyPair? {
        return try {
            val keyGen = KeyPairGenerator.getInstance("RSA")
            keyGen.initialize(RSA_KEY_SIZE_BITS, SecureRandom())
            keyGen.generateKeyPair()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }



    @JvmStatic
    fun encryptPrivateKeyWithPassword(priv: PrivateKey, password: String): String {
        val result = hashPasswordForKeyWrap(password)
        val aesKey = result.hash

        val pem = pemEncodeRsaKey(priv)
        val aesPlaintextBytes = pem.toByteArray(StandardCharsets.UTF_8)
        val aesCiphertextBytes = encryptWithAes(aesPlaintextBytes, aesKey)!!

        val concat = concatByteArrays(result.params.salt, aesCiphertextBytes)
        return encodeBase64(concat)
    }

    @JvmStatic
    fun decryptPrivateKeyWithPassword(encryptedPrivKey: String, password: String): KeyPair? {
        val concatBytes = decodeBase64(encryptedPrivKey)
        val saltBytes = concatBytes.copyOfRange(0, ARGON2_SALT_LENGTH)
        val ciphertextBytes = concatBytes.copyOfRange(ARGON2_SALT_LENGTH, concatBytes.size)

        val result = hashPasswordForKeyWrap(password, saltBytes)
        val aesKey = result.hash

        val aesPlaintextBytes = decryptWithAes(ciphertextBytes, aesKey) ?: return null
        val pem = String(aesPlaintextBytes, StandardCharsets.UTF_8)
        return pemDecodeRsaPrivateKey(pem)
    }

    @JvmStatic
    fun pemEncodeRsaKey(priv: PrivateKey): String {
        val sw = StringWriter()
        val writer = PemWriter(sw)
        val po = PemObject("PRIVATE KEY", priv.encoded)
        try {
            writer.writeObject(po)
            writer.flush()
            writer.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return sw.buffer.toString()
    }

    @JvmStatic
    fun pemDecodeRsaPrivateKey(pemInput: String): KeyPair? {
        var pem = pemInput
        return try {
            pem = pem.replace("-----END PRIVATE KEY-----\n", "")
            pem = pem.replace("-----BEGIN PRIVATE KEY-----\n", "")
            pem = pem.replace("\n", "")
            val key = decodeBase64(pem)

            val privKeySpec: EncodedKeySpec = PKCS8EncodedKeySpec(key)
            val keyFactory = KeyFactory.getInstance("RSA")
            val priv = keyFactory.generatePrivate(privKeySpec)

            val publicKeySpec = if (priv is RSAPrivateCrtKey) {
                RSAPublicKeySpec(priv.modulus, priv.publicExponent)
            } else {
                val rsaPriv = priv as RSAPrivateKey
                RSAPublicKeySpec(rsaPriv.modulus, BigInteger.valueOf(65537))
            }
            val pub = keyFactory.generatePublic(publicKeySpec)

            KeyPair(pub, priv)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    @JvmStatic
    fun decodeRsaPublicKey(base64: String): PublicKey? {
        return try {
            val keyBytes = decodeBase64(base64)
            val keySpec: EncodedKeySpec = X509EncodedKeySpec(keyBytes)
            val keyFactory = KeyFactory.getInstance("RSA")
            keyFactory.generatePublic(keySpec)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }



    @JvmStatic
    fun encryptWithAes(msgBytes: ByteArray, aesKey: ByteArray): ByteArray? {
        if (aesKey.size != AES_GCM_KEY_SIZE_BYTES) {
            throw RuntimeException("Bad AES key size:${aesKey.size}")
        }
        return try {
            val ivBytes = generateSecureRandom(AES_GCM_IV_SIZE_BYTES)
            val gcmSpec = GCMParameterSpec(AES_GCM_TAG_SIZE_BITS, ivBytes)
            val secretKeySpec = SecretKeySpec(aesKey, "AES")

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, gcmSpec)
            val ctBytes = cipher.doFinal(msgBytes)

            concatByteArrays(ivBytes, ctBytes)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    @JvmStatic
    fun decryptWithAes(msgBytes: ByteArray, aesKey: ByteArray): ByteArray? {
        return try {
            val ivBytes = msgBytes.copyOfRange(0, AES_GCM_IV_SIZE_BYTES)
            val ctBytes = msgBytes.copyOfRange(AES_GCM_IV_SIZE_BYTES, msgBytes.size)

            val gcmSpec = GCMParameterSpec(AES_GCM_TAG_SIZE_BITS, ivBytes)
            val secretKeySpec = SecretKeySpec(aesKey, "AES")

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, gcmSpec)
            cipher.doFinal(ctBytes)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    @JvmStatic
    fun encodeBase64(toEncode: ByteArray): String {
        return Base64.encodeToString(toEncode, Base64.DEFAULT)
    }

    @JvmStatic
    fun decodeBase64(toDecode: String): ByteArray {
        return Base64.decode(toDecode, Base64.DEFAULT)
    }

    @JvmStatic
    fun generateSecureRandom(lengthInBytes: Int): ByteArray {
        val random = SecureRandom()
        val randomBytes = ByteArray(lengthInBytes)
        random.nextBytes(randomBytes)
        return randomBytes
    }

    @JvmStatic
    fun concatByteArrays(vararg arrays: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        for (array in arrays) {
            try {
                out.write(array)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return out.toByteArray()
    }
}
