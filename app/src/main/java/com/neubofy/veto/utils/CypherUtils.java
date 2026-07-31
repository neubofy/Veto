package com.neubofy.veto.utils;

import android.util.Base64;

import androidx.annotation.VisibleForTesting;

import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.PSSParameterSpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource.PSpecified;
import javax.crypto.spec.SecretKeySpec;


public class CypherUtils {

    // https://pages.nist.gov/800-63-4/sp800-63b.html#passwordver
    public static final int MIN_PASSWORD_LENGTH = 8;

    private static final int AES_GCM_IV_SIZE_BYTES = 12; // byte = 96 bit
    @VisibleForTesting
    protected static final int AES_GCM_KEY_SIZE_BYTES = 32; // byte = 256 bit
    private static final int AES_GCM_TAG_SIZE_BITS = 128; // bit = 16 byte

    private static final int RSA_KEY_SIZE_BITS = 3072;

    // Argon2: see the PROTOCOL.md
    private static final int ARGON2_T = 1;
    private static final int ARGON2_P = 4;
    private static final int ARGON2_M = 131072;
    private static final int ARGON2_HASH_LENGTH = 32; // byte = 256 bit
    private static final int ARGON2_SALT_LENGTH = 16; // byte = 128 bit

    // Contextualise all usages of Argon2 to provide some hacky key separation
    private static final String CONTEXT_STRING_ASYM_KEY_WRAP = "context:asymmetricKeyWrap";
    private static final String CONTEXT_STRING_Veto_PIN = "context:vetoPin";
    private static final String CONTEXT_STRING_LOGIN = "context:loginAuthentication";
    private static final String CONTEXT_PREFIX = "context:";

    // ------ Section: Password and hashing ------

    public static String hashPasswordForVetoPin(String password) {
        password = CONTEXT_STRING_Veto_PIN + password;
        byte[] salt = generateSecureRandom(ARGON2_SALT_LENGTH);
        Argon2Result result = hashPasswordArgon2(password, salt);
        return Argon2EncodingUtils.encode(result.hash, result.params);
    }

    public static String hashPasswordForLogin(String password) {
        byte[] salt = generateSecureRandom(ARGON2_SALT_LENGTH);
        return hashPasswordForLogin(password, salt);
    }

    public static String hashPasswordForLogin(String password, String saltBase64) {
        // Must use the same decoding flags as the encoder.
        // If we used `DatatypeConverter.parseBase64Binary(toDecode)` we would lose a byte.
        byte[] salt = Base64.decode(saltBase64, Argon2EncodingUtils.BASE64_FLAGS);
        return hashPasswordForLogin(password, salt);
    }

    public static String hashPasswordForLogin(String password, byte[] saltBytes) {
        password = CONTEXT_STRING_LOGIN + password;
        Argon2Result result = hashPasswordArgon2(password, saltBytes);
        return Argon2EncodingUtils.encode(result.hash, result.params);
    }

    public static Argon2Result hashPasswordForKeyWrap(String password) {
        byte[] salt = generateSecureRandom(ARGON2_SALT_LENGTH);
        return hashPasswordForKeyWrap(password, salt);
    }

    public static Argon2Result hashPasswordForKeyWrap(String password, byte[] saltBytes) {
        password = CONTEXT_STRING_ASYM_KEY_WRAP + password;
        return hashPasswordArgon2(password, saltBytes);
    }

    private static Argon2Result hashPasswordArgon2(String password, byte[] salt) {
        // Inspired by https://github.com/spring-projects/spring-security/blob/6.1.0/crypto/src/main/java/org/springframework/security/crypto/argon2/Argon2PasswordEncoder.java
        // and https://www.baeldung.com/java-argon2-hashing#2-implement-argon2-hashing-with-bouncy-castle
        if (!password.startsWith(CONTEXT_PREFIX)) {
            // This is a bug that should not happen
            // Be defensive to ensure all Argon2 usages are context-separated.
            throw new RuntimeException("Missing context string");
        }
        byte[] passwordBytes = password.getBytes(StandardCharsets.UTF_8);
        byte[] out = new byte[ARGON2_HASH_LENGTH];

        Argon2Parameters params = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withVersion(Argon2Parameters.ARGON2_VERSION_13)
                .withIterations(ARGON2_T)
                .withParallelism(ARGON2_P)
                .withMemoryAsKB(ARGON2_M)
                .withSalt(salt)
                .build();

        Argon2BytesGenerator generator = new Argon2BytesGenerator();
        generator.init(params);
        generator.generateBytes(passwordBytes, out);

        return new Argon2Result(out, params);
    }

    public static boolean checkPasswordForVetoPin(String expectedHash, String password) {
        return checkPassword(expectedHash, CONTEXT_STRING_Veto_PIN + password);
    }

    public static boolean checkPasswordForLogin(String expectedHash, String password) {
        return checkPassword(expectedHash, CONTEXT_STRING_LOGIN + password);
    }

    private static boolean checkPassword(String expectedHash, String password) {
        if (expectedHash.isEmpty() || password.isEmpty()) {
            return false;
        }

        Argon2EncodingUtils.Argon2Hash decodedExpected;
        try {
            decodedExpected = Argon2EncodingUtils.decode(expectedHash);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return false;
        }

        byte[] passwordBytes = password.getBytes(StandardCharsets.UTF_8);
        byte[] actualBytes = new byte[decodedExpected.getHash().length];

        Argon2BytesGenerator generator = new Argon2BytesGenerator();
        generator.init(decodedExpected.getParameters());
        generator.generateBytes(passwordBytes, actualBytes);

        return Arrays.constantTimeAreEqual(decodedExpected.getHash(), actualBytes);
    }

    // ------ Section: asymmetric key ------

    public static KeyPair genRsaKeyPair() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(RSA_KEY_SIZE_BITS, new SecureRandom());
            return keyGen.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Note that we use SHA-256 for MGF1 (not SHA-1). This is because the WebCrypto API only works with SHA-256.
    // See https://www.w3.org/2014/01/W3C_Web_Crypto_API_status_january_2014.pdf?page=8
    private static final OAEPParameterSpec OAEP_PARAMS = new OAEPParameterSpec("SHA-256", "MGF1", new MGF1ParameterSpec("SHA-256"), PSpecified.DEFAULT);

    public static byte[] encryptWithKey(PublicKey pub, String msg) {
        byte[] sessionKey = generateSecureRandom(AES_GCM_KEY_SIZE_BYTES);

        try {
            // Symmetrically encrypt message
            byte[] msgBytes = msg.getBytes(StandardCharsets.UTF_8);
            byte[] ivAndAesCiphertext = encryptWithAes(msgBytes, sessionKey);

            // Wrap key
            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPPadding");
            cipher.init(Cipher.ENCRYPT_MODE, pub, OAEP_PARAMS); // XXX: should use WRAP_MODE
            byte[] sessionKeyPacket = cipher.doFinal(sessionKey);

            return concatByteArrays(sessionKeyPacket, ivAndAesCiphertext);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                 BadPaddingException | IllegalBlockSizeException |
                 InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String decryptWithKey(PrivateKey priv, byte[] encryptedMsg) {
        byte[] sessionKeyPacket = Arrays.copyOfRange(encryptedMsg, 0, RSA_KEY_SIZE_BITS / 8);
        byte[] ivAndAesCiphertext = Arrays.copyOfRange(encryptedMsg, RSA_KEY_SIZE_BITS / 8, encryptedMsg.length);

        try {
            // Unwrap key
            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPPadding");
            cipher.init(Cipher.DECRYPT_MODE, priv, OAEP_PARAMS); // XXX: should use UNWRAP_MODE
            byte[] sessionKey = cipher.doFinal(sessionKeyPacket);

            // Symmetrically decrypt message
            byte[] msg = decryptWithAes(ivAndAesCiphertext, sessionKey);
            return new String(msg, StandardCharsets.UTF_8);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | BadPaddingException |
                 IllegalBlockSizeException | InvalidKeyException |
                 InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }
        return null;
    }

    // ------ Section: asymmetric key (key wrap) ------

    public static String encryptPrivateKeyWithPassword(PrivateKey priv, String password) {
        Argon2Result result = hashPasswordForKeyWrap(password);
        byte[] aesKey = result.hash;

        // PEM-encode the key and symmetrically encrypt
        String pem = pemEncodeRsaKey(priv);
        byte[] aesPlaintextBytes = pem.getBytes(StandardCharsets.UTF_8);
        byte[] aesCiphertextBytes = encryptWithAes(aesPlaintextBytes, aesKey);

        byte[] concat = concatByteArrays(result.params.getSalt(), aesCiphertextBytes);
        return encodeBase64(concat);
    }

    public static KeyPair decryptPrivateKeyWithPassword(String encryptedPrivKey, String password) {
        byte[] concatBytes = decodeBase64(encryptedPrivKey);
        byte[] saltBytes = Arrays.copyOfRange(concatBytes, 0, ARGON2_SALT_LENGTH);
        byte[] ciphertextBytes = Arrays.copyOfRange(concatBytes, ARGON2_SALT_LENGTH, concatBytes.length);

        Argon2Result result = hashPasswordForKeyWrap(password, saltBytes);
        byte[] aesKey = result.hash;

        byte[] aesPlaintextBytes = decryptWithAes(ciphertextBytes, aesKey);
        String pem = new String(aesPlaintextBytes, StandardCharsets.UTF_8);
        return pemDecodeRsaPrivateKey(pem);
    }

    public static String pemEncodeRsaKey(PrivateKey priv) {
        StringWriter sw = new StringWriter();
        PemWriter writer = new PemWriter(sw);
        PemObject po = new PemObject("PRIVATE KEY", priv.getEncoded());
        try {
            writer.writeObject(po);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sw.getBuffer().toString();
    }

    public static KeyPair pemDecodeRsaPrivateKey(String pem) {
        try {
            pem = pem.replace("-----END PRIVATE KEY-----\n", "");
            pem = pem.replace("-----BEGIN PRIVATE KEY-----\n", "");
            pem = pem.replace("\n", "");
            byte[] key = decodeBase64(pem);

            EncodedKeySpec privKeySpec = new PKCS8EncodedKeySpec(key);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PrivateKey priv = keyFactory.generatePrivate(privKeySpec);

            RSAPublicKeySpec publicKeySpec;
            if (priv instanceof RSAPrivateCrtKey) {
                RSAPrivateCrtKey rsaPriv = (RSAPrivateCrtKey) priv;
                publicKeySpec = new RSAPublicKeySpec(rsaPriv.getModulus(), rsaPriv.getPublicExponent());
            } else {
                // Fallback for ROMs that return an OpenSSLRSAPrivateKey (which is not an instance of RSAPrivateCrtKey).
                // Guessing the exponent is not great, but for RSA it should work in most cases.
                // https://gitlab.com/veto-foss/veto-android/-/issues/389
                // https://github.com/google/conscrypt/blob/master/common/src/main/java/org/conscrypt/OpenSSLRSAPrivateKey.java
                RSAPrivateKey rsaPriv = (RSAPrivateKey) priv;
                publicKeySpec = new RSAPublicKeySpec(rsaPriv.getModulus(), BigInteger.valueOf(65537));
            }
            PublicKey pub = keyFactory.generatePublic(publicKeySpec);

            return new KeyPair(pub, priv);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | NullPointerException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static PublicKey decodeRsaPublicKey(String base64) {
        try {
            byte[] keyBytes = decodeBase64(base64);
            EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePublic(keySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | NullPointerException e) {
            e.printStackTrace();
        }
        return null;
    }

    // ------ Section: asymmetric key (signature) ------

    public static boolean verifySig(String publicKeyBase64, String msg, String sigBase64) {
        PublicKey pubKey = decodeRsaPublicKey(publicKeyBase64);
        byte[] msgBytes = msg.getBytes(StandardCharsets.UTF_8);
        byte[] sigBytes = Base64.decode(sigBase64, Base64.DEFAULT);

        try {
            Signature sig = Signature.getInstance("SHA256withRSA/PSS");
            sig.setParameter(new PSSParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, 32, 1));
            sig.initVerify(pubKey);
            sig.update(msgBytes);
            return sig.verify(sigBytes);
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException |
                 InvalidKeyException | SignatureException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ------ Section: symmetric key ------

    public static byte[] encryptWithAes(byte[] msgBytes, byte[] aesKey) {
        if (aesKey.length != AES_GCM_KEY_SIZE_BYTES) {
            // This is a bug
            throw new RuntimeException("Bad AES key size:" + aesKey.length);
        }
        try {
            byte[] ivBytes = generateSecureRandom(AES_GCM_IV_SIZE_BYTES);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(AES_GCM_TAG_SIZE_BITS, ivBytes);
            SecretKeySpec secretKeySpec = new SecretKeySpec(aesKey, "AES");

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, gcmSpec);
            byte[] ctBytes = cipher.doFinal(msgBytes);

            return concatByteArrays(ivBytes, ctBytes);

        } catch (NoSuchAlgorithmException | NoSuchPaddingException |
                 InvalidKeyException | IllegalBlockSizeException | BadPaddingException |
                 InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }
        return null;
    }

    protected static byte[] decryptWithAes(byte[] msgBytes, byte[] aesKey) {
        try {
            byte[] ivBytes = Arrays.copyOfRange(msgBytes, 0, AES_GCM_IV_SIZE_BYTES);
            byte[] ctBytes = Arrays.copyOfRange(msgBytes, AES_GCM_IV_SIZE_BYTES, msgBytes.length);

            GCMParameterSpec gcmSpec = new GCMParameterSpec(AES_GCM_TAG_SIZE_BITS, ivBytes);
            SecretKeySpec secretKeySpec = new SecretKeySpec(aesKey, "AES");

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, gcmSpec);
            return cipher.doFinal(ctBytes);

        } catch (NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException |
                 BadPaddingException | IllegalBlockSizeException |
                 InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }
        return null;
    }

    // ------ Section: utils ------

    public static String encodeBase64(byte[] toEncode) {
        return Base64.encodeToString(toEncode, Base64.DEFAULT);
    }

    public static byte[] decodeBase64(String toDecode) {
        return Base64.decode(toDecode, Base64.DEFAULT);
    }

    public static byte[] generateSecureRandom(int lengthInBytes) {
        SecureRandom random = new SecureRandom();
        byte[] randomBytes = new byte[lengthInBytes];
        random.nextBytes(randomBytes);
        return randomBytes;
    }

    public static byte[] concatByteArrays(byte[]... arrays) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (byte[] array : arrays) {
            try {
                out.write(array);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return out.toByteArray();
    }

    public static class Argon2Result {
        public final byte[] hash;
        public final Argon2Parameters params;


        public Argon2Result(byte[] hash, Argon2Parameters params) {
            this.hash = hash;
            this.params = params;
        }
    }

}
