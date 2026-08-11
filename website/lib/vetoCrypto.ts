import crypto from 'crypto';

const PBKDF2_ITERATIONS = 100000;
const KEY_LENGTH = 32; // 256-bit key
const IV_LENGTH = 12; // 96-bit IV for GCM
const TAG_LENGTH = 16; // 128-bit auth tag

/**
 * Derives a key using PBKDF2-HMAC-SHA256 from the raw PIN and user UID.
 * 
 * @param rawPin The user's Veto PIN
 * @param uid The user's Firebase UID (used as salt)
 * @returns The derived 32-byte key
 */
function deriveKey(rawPin: string, uid: string): Buffer {
    const salt = Buffer.from(uid, 'utf-8');
    return crypto.pbkdf2Sync(rawPin, salt, PBKDF2_ITERATIONS, KEY_LENGTH, 'sha256');
}

/**
 * Encrypts a plaintext string using AES-256-GCM.
 * 
 * @param plaintext The text to encrypt
 * @param rawPin The user's Veto PIN
 * @param uid The user's Firebase UID
 * @returns A base64-encoded string containing the IV, ciphertext, and auth tag
 */
export function encryptVeto(plaintext: string, rawPin: string, uid: string): string {
    const key = deriveKey(rawPin, uid);
    const iv = crypto.randomBytes(IV_LENGTH);
    
    const cipher = crypto.createCipheriv('aes-256-gcm', key, iv);
    
    let ciphertext = cipher.update(plaintext, 'utf8', 'base64');
    ciphertext += cipher.final('base64');
    
    const tag = cipher.getAuthTag();
    
    // Format: iv(base64):ciphertext:tag(base64)
    return `${iv.toString('base64')}:${ciphertext}:${tag.toString('base64')}`;
}

/**
 * Decrypts a Veto-encrypted string using AES-256-GCM.
 * 
 * @param encryptedData The base64-encoded string containing the IV, ciphertext, and auth tag
 * @param rawPin The user's Veto PIN
 * @param uid The user's Firebase UID
 * @returns The decrypted plaintext
 */
export function decryptVeto(encryptedData: string, rawPin: string, uid: string): string {
    const parts = encryptedData.split(':');
    if (parts.length !== 3) {
        throw new Error('Invalid encrypted data format');
    }
    
    const iv = Buffer.from(parts[0], 'base64');
    const ciphertext = parts[1];
    const tag = Buffer.from(parts[2], 'base64');
    
    const key = deriveKey(rawPin, uid);
    
    const decipher = crypto.createDecipheriv('aes-256-gcm', key, iv);
    decipher.setAuthTag(tag);
    
    let plaintext = decipher.update(ciphertext, 'base64', 'utf8');
    plaintext += decipher.final('utf8');
    
    return plaintext;
}
