package com.neubofy.veto.utils

import android.util.Base64
import org.bouncycastle.crypto.params.Argon2Parameters
import java.nio.charset.StandardCharsets

object Argon2EncodingUtils {
    const val BASE64_FLAGS = Base64.NO_PADDING or Base64.NO_WRAP

    class Argon2Hash(val hash: ByteArray, val parameters: Argon2Parameters)

    @JvmStatic
    fun encode(hash: ByteArray, parameters: Argon2Parameters): String {
        val b64Salt = Base64.encodeToString(parameters.salt, BASE64_FLAGS)
        val b64Hash = Base64.encodeToString(hash, BASE64_FLAGS)
        val type = when (parameters.type) {
            Argon2Parameters.ARGON2_d -> "argon2d"
            Argon2Parameters.ARGON2_i -> "argon2i"
            Argon2Parameters.ARGON2_id -> "argon2id"
            else -> throw IllegalArgumentException("Invalid type: ${parameters.type}")
        }
        return "\$$type\$v=${parameters.version}\$m=${parameters.memory},t=${parameters.iterations},p=${parameters.lanes}\$$b64Salt\$$b64Hash"
    }

    @JvmStatic
    fun decode(encoded: String): Argon2Hash {
        val parts = encoded.split("$").filter { it.isNotEmpty() }
        if (parts.size < 5) {
            throw IllegalArgumentException("Invalid encoded Argon2 string")
        }

        val type = when (parts[0]) {
            "argon2d" -> Argon2Parameters.ARGON2_d
            "argon2i" -> Argon2Parameters.ARGON2_i
            "argon2id" -> Argon2Parameters.ARGON2_id
            else -> throw IllegalArgumentException("Invalid type: ${parts[0]}")
        }

        val version = parts[1].replace("v=", "").toInt()
        val paramsParts = parts[2].split(",")
        val memory = paramsParts[0].replace("m=", "").toInt()
        val iterations = paramsParts[1].replace("t=", "").toInt()
        val parallelism = paramsParts[2].replace("p=", "").toInt()

        val salt = Base64.decode(parts[3], BASE64_FLAGS)
        val hash = Base64.decode(parts[4], BASE64_FLAGS)

        val parameters = Argon2Parameters.Builder(type)
            .withVersion(version)
            .withMemoryAsKB(memory)
            .withIterations(iterations)
            .withParallelism(parallelism)
            .withSalt(salt)
            .build()

        return Argon2Hash(hash, parameters)
    }
}
