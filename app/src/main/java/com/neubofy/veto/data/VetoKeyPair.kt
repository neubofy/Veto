package com.neubofy.veto.data

import com.neubofy.veto.utils.CypherUtils
import java.security.KeyPair
import java.security.PublicKey

class VetoKeyPair {
    var publicKey: PublicKey
        private set
    var encryptedPrivateKey: String
        private set

    constructor(publicKey: PublicKey, encryptedPrivateKey: String) {
        this.publicKey = publicKey
        this.encryptedPrivateKey = encryptedPrivateKey
    }

    constructor(rsaKeyPair: KeyPair, passwordProtectKeyPairWith: String) {
        val encrypted = CypherUtils.encryptPrivateKeyWithPassword(rsaKeyPair.private, passwordProtectKeyPairWith)
        this.publicKey = rsaKeyPair.public
        this.encryptedPrivateKey = encrypted
    }

    val base64PublicKey: String
        get() = CypherUtils.encodeBase64(publicKey.encoded)

    companion object {
        @JvmStatic
        fun generateNewVetoKeyPair(passwordProtectKeyPairWith: String): VetoKeyPair {
            val rsaKeyPair = CypherUtils.genRsaKeyPair() ?: throw IllegalStateException("Failed to generate RSA keypair")
            val encrypted = CypherUtils.encryptPrivateKeyWithPassword(rsaKeyPair.private, passwordProtectKeyPairWith)
            return VetoKeyPair(rsaKeyPair.public, encrypted)
        }
    }
}
