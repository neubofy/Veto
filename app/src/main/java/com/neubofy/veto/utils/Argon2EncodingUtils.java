package com.neubofy.veto.utils;
// Taken from Spring Security, published under Apache License, Version 2.0.
// Patched the Base64 to use Android's classes. Their java.util.Base64 is only available on API level >= 26.
// https://github.com/spring-projects/spring-security/blob/6.1.0/crypto/src/main/java/org/springframework/security/crypto/argon2/Argon2EncodingUtils.java

/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


// BEGIN PATCH
// import java.util.Base64;
import android.util.Base64;
// END PATCH

import org.bouncycastle.crypto.params.Argon2Parameters;
import org.bouncycastle.util.Arrays;

/**
 * Utility for encoding and decoding Argon2 hashes.
 *
 * Used by {@link Argon2PasswordEncoder}.
 *
 * @author Simeon Macke
 * @since 5.3
 */
final class Argon2EncodingUtils {

    // BEGIN PATCH
    // private static final Base64.Encoder b64encoder = Base64.getEncoder().withoutPadding();

    // private static final Base64.Decoder b64decoder = Base64.getDecoder();
    public static final int BASE64_FLAGS = Base64.NO_PADDING | Base64.NO_WRAP;
    // END PATCH

    private Argon2EncodingUtils() {
    }

    /**
     * Encodes a raw Argon2-hash and its parameters into the standard Argon2-hash-string
     * as specified in the reference implementation
     * (https://github.com/P-H-C/phc-winner-argon2/blob/master/src/encoding.c#L244):
     *
     * {@code $argon2<T>[$v=<num>]$m=<num>,t=<num>,p=<num>$<bin>$<bin>}
     *
     * where {@code <T>} is either 'd', 'id', or 'i', {@code <num>} is a decimal integer
     * (positive, fits in an 'unsigned long'), and {@code <bin>} is Base64-encoded data
     * (no '=' padding characters, no newline or whitespace).
     *
     * The last two binary chunks (encoded in Base64) are, in that order, the salt and the
     * output. If no salt has been used, the salt will be omitted.
     * @param hash the raw Argon2 hash in binary format
     * @param parameters the Argon2 parameters that were used to create the hash
     * @return the encoded Argon2-hash-string as described above
     * @throws IllegalArgumentException if the Argon2Parameters are invalid
     */
    static String encode(byte[] hash, Argon2Parameters parameters) throws IllegalArgumentException {
        StringBuilder stringBuilder = new StringBuilder();
        switch (parameters.getType()) {
            case Argon2Parameters.ARGON2_d:
                stringBuilder.append("$argon2d");
                break;
            case Argon2Parameters.ARGON2_i:
                stringBuilder.append("$argon2i");
                break;
            case Argon2Parameters.ARGON2_id:
                stringBuilder.append("$argon2id");
                break;
            default:
                throw new IllegalArgumentException("Invalid algorithm type: " + parameters.getType());
        }
        stringBuilder.append("$v=").append(parameters.getVersion()).append("$m=").append(parameters.getMemory())
                .append(",t=").append(parameters.getIterations()).append(",p=").append(parameters.getLanes());
        if (parameters.getSalt() != null) {
            // BEGIN PATCH
            // stringBuilder.append("$").append(b64encoder.encodeToString(parameters.getSalt()));
            stringBuilder.append("$").append(Base64.encodeToString(parameters.getSalt(), BASE64_FLAGS));
        }
        // stringBuilder.append("$").append(b64encoder.encodeToString(hash));
        stringBuilder.append("$").append(Base64.encodeToString(hash, BASE64_FLAGS));
        // END PATCH
        return stringBuilder.toString();
    }

    /**
     * Decodes an Argon2 hash string as specified in the reference implementation
     * (https://github.com/P-H-C/phc-winner-argon2/blob/master/src/encoding.c#L244) into
     * the raw hash and the used parameters.
     *
     * The hash has to be formatted as follows:
     * {@code $argon2<T>[$v=<num>]$m=<num>,t=<num>,p=<num>$<bin>$<bin>}
     *
     * where {@code <T>} is either 'd', 'id', or 'i', {@code <num>} is a decimal integer
     * (positive, fits in an 'unsigned long'), and {@code <bin>} is Base64-encoded data
     * (no '=' padding characters, no newline or whitespace).
     *
     * The last two binary chunks (encoded in Base64) are, in that order, the salt and the
     * output. Both are required. The binary salt length and the output length must be in
     * the allowed ranges defined in argon2.h.
     * @param encodedHash the Argon2 hash string as described above
     * @return an {@link org.springframework.security.crypto.argon2.Argon2EncodingUtils.Argon2Hash} object containing the raw hash and the
     * {@link Argon2Parameters}.
     * @throws IllegalArgumentException if the encoded hash is malformed
     */
    static Argon2EncodingUtils.Argon2Hash decode(String encodedHash) throws IllegalArgumentException {
        Argon2Parameters.Builder paramsBuilder;
        String[] parts = encodedHash.split("\\$");
        if (parts.length < 4) {
            throw new IllegalArgumentException("Invalid encoded Argon2-hash");
        }
        int currentPart = 1;
        switch (parts[currentPart++]) {
            case "argon2d":
                paramsBuilder = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_d);
                break;
            case "argon2i":
                paramsBuilder = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_i);
                break;
            case "argon2id":
                paramsBuilder = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id);
                break;
            default:
                throw new IllegalArgumentException("Invalid algorithm type: " + parts[0]);
        }
        if (parts[currentPart].startsWith("v=")) {
            paramsBuilder.withVersion(Integer.parseInt(parts[currentPart].substring(2)));
            currentPart++;
        }
        String[] performanceParams = parts[currentPart++].split(",");
        if (performanceParams.length != 3) {
            throw new IllegalArgumentException("Amount of performance parameters invalid");
        }
        if (!performanceParams[0].startsWith("m=")) {
            throw new IllegalArgumentException("Invalid memory parameter");
        }
        paramsBuilder.withMemoryAsKB(Integer.parseInt(performanceParams[0].substring(2)));
        if (!performanceParams[1].startsWith("t=")) {
            throw new IllegalArgumentException("Invalid iterations parameter");
        }
        paramsBuilder.withIterations(Integer.parseInt(performanceParams[1].substring(2)));
        if (!performanceParams[2].startsWith("p=")) {
            throw new IllegalArgumentException("Invalid parallelity parameter");
        }
        paramsBuilder.withParallelism(Integer.parseInt(performanceParams[2].substring(2)));
        // BEGIN PATCH
        // paramsBuilder.withSalt(b64decoder.decode(parts[currentPart++]));
        paramsBuilder.withSalt(Base64.decode(parts[currentPart++], BASE64_FLAGS));
        // return new Argon2EncodingUtils.Argon2Hash(b64decoder.decode(parts[currentPart]), paramsBuilder.build());
        return new Argon2EncodingUtils.Argon2Hash(Base64.decode(parts[currentPart], BASE64_FLAGS), paramsBuilder.build());
        // END PATCH
    }

    public static class Argon2Hash {

        private byte[] hash;

        private Argon2Parameters parameters;

        Argon2Hash(byte[] hash, Argon2Parameters parameters) {
            this.hash = Arrays.clone(hash);
            this.parameters = parameters;
        }

        public byte[] getHash() {
            return Arrays.clone(this.hash);
        }

        public void setHash(byte[] hash) {
            this.hash = Arrays.clone(hash);
        }

        public Argon2Parameters getParameters() {
            return this.parameters;
        }

        public void setParameters(Argon2Parameters parameters) {
            this.parameters = parameters;
        }

    }

}

