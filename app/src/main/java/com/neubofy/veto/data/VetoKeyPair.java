package com.neubofy.veto.data;

import java.security.KeyPair;
import java.security.PublicKey;

import com.neubofy.veto.utils.CypherUtils;

public class VetoKeyPair {
    private PublicKey publicKey;
    private String encryptedPrivateKey;

    // TODO make private
    public VetoKeyPair(PublicKey publicKey, String encryptedPrivateKey) {
        this.publicKey = publicKey;
        this.encryptedPrivateKey = encryptedPrivateKey;
    }

    public VetoKeyPair(KeyPair rsaKeyPair, String passwordProtectKeyPairWith) {
        String encryptedPrivateKey = CypherUtils.encryptPrivateKeyWithPassword(rsaKeyPair.getPrivate(), passwordProtectKeyPairWith);
        this.publicKey = rsaKeyPair.getPublic();
        this.encryptedPrivateKey = encryptedPrivateKey;
    }

    public static VetoKeyPair generateNewVetoKeyPair(String passwordProtectKeyPairWith) {
        KeyPair rsaKeyPair = CypherUtils.genRsaKeyPair();
        String encryptedPrivateKey = CypherUtils.encryptPrivateKeyWithPassword(rsaKeyPair.getPrivate(), passwordProtectKeyPairWith);
        return new VetoKeyPair(rsaKeyPair.getPublic(), encryptedPrivateKey);
    }

    public PublicKey getPublicKey() {
        return this.publicKey;
    }

    public void setPublicKey(PublicKey publicKey) {
        this.publicKey = publicKey;
    }

    public String getEncryptedPrivateKey() {
        return this.encryptedPrivateKey;
    }

    public String getBase64PublicKey() {
        return CypherUtils.encodeBase64(publicKey.getEncoded());
    }

    public void setEncryptedPrivateKey(String encryptedPrivateKey) {
        this.encryptedPrivateKey = encryptedPrivateKey;
    }
}
