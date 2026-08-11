/**
 * Client-side End-to-End Encryption utility using Web Crypto API
 * This ensures that the Veto PIN and unencrypted data never leave the browser.
 */

const ITERATIONS = 100000;

function arrayBufferToBase64(buffer: ArrayBuffer): string {
    let binary = '';
    const bytes = new Uint8Array(buffer);
    const len = bytes.byteLength;
    for (let i = 0; i < len; i++) {
        binary += String.fromCharCode(bytes[i]);
    }
    return window.btoa(binary);
}

function base64ToArrayBuffer(base64: string): ArrayBuffer {
    const binary_string = window.atob(base64);
    const len = binary_string.length;
    const bytes = new Uint8Array(len);
    for (let i = 0; i < len; i++) {
        bytes[i] = binary_string.charCodeAt(i);
    }
    return bytes.buffer;
}

export async function deriveKey(pin: string, uid: string): Promise<CryptoKey> {
    const enc = new TextEncoder();
    const keyMaterial = await window.crypto.subtle.importKey(
        "raw",
        enc.encode(pin),
        { name: "PBKDF2" },
        false,
        ["deriveBits", "deriveKey"]
    );

    return window.crypto.subtle.deriveKey(
        {
            name: "PBKDF2",
            salt: enc.encode(uid),
            iterations: ITERATIONS,
            hash: "SHA-256"
        },
        keyMaterial,
        { name: "AES-GCM", length: 256 },
        false,
        ["encrypt", "decrypt"]
    );
}

export async function encryptClient(plaintext: string, pin: string, uid: string): Promise<string> {
    const key = await deriveKey(pin, uid);
    const iv = window.crypto.getRandomValues(new Uint8Array(12));
    const enc = new TextEncoder();
    
    const ciphertextBuf = await window.crypto.subtle.encrypt(
        {
            name: "AES-GCM",
            iv: iv
        },
        key,
        enc.encode(plaintext)
    );
    
    // Web Crypto API automatically appends the 16-byte auth tag to the end of the ciphertext.
    // This perfectly matches Java's Cipher.doFinal behaviour.
    const ivBase64 = arrayBufferToBase64(iv.buffer);
    const cipherBase64 = arrayBufferToBase64(ciphertextBuf);
    
    return `${ivBase64}:${cipherBase64}`;
}

export async function decryptClient(encryptedData: string, pin: string, uid: string): Promise<string> {
    const parts = encryptedData.split(':');
    if (parts.length !== 2) {
        throw new Error('Invalid encrypted data format');
    }
    
    const iv = base64ToArrayBuffer(parts[0]);
    const ciphertext = base64ToArrayBuffer(parts[1]);
    const key = await deriveKey(pin, uid);
    
    const decryptedBuf = await window.crypto.subtle.decrypt(
        {
            name: "AES-GCM",
            iv: new Uint8Array(iv)
        },
        key,
        ciphertext
    );
    
    const dec = new TextDecoder();
    return dec.decode(decryptedBuf);
}
