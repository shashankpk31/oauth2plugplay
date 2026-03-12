package com.auth.identity.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Utility class for encrypting and decrypting sensitive data like MPIN
 * Uses AES-256-GCM encryption for strong security
 */
@Slf4j
@Component
public class EncryptionUtil {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12; // 96 bits
    private static final int GCM_TAG_LENGTH = 128; // 128 bits

    private final SecretKey secretKey;
    private final SecureRandom secureRandom;

    public EncryptionUtil(@Value("${mpin.encryption.key}") String base64Key) {
        try {
            // Decode the base64 key
            byte[] decodedKey = Base64.getDecoder().decode(base64Key);

            // Ensure key is 32 bytes (256 bits) for AES-256
            if (decodedKey.length != 32) {
                throw new IllegalArgumentException(
                    "Encryption key must be 32 bytes (256 bits). Current length: " + decodedKey.length
                );
            }

            this.secretKey = new SecretKeySpec(decodedKey, "AES");
            this.secureRandom = new SecureRandom();

            log.info("EncryptionUtil initialized successfully with AES-256-GCM");
        } catch (Exception e) {
            log.error("Failed to initialize EncryptionUtil", e);
            throw new RuntimeException("Failed to initialize encryption", e);
        }
    }

    /**
     * Encrypts the given plaintext using AES-256-GCM
     * Returns base64-encoded string containing: IV + Encrypted Data
     *
     * @param plaintext The text to encrypt
     * @return Base64-encoded encrypted data with IV
     */
    public String encrypt(String plaintext) {
        try {
            // Generate random IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            // Initialize cipher for encryption
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            // Encrypt the plaintext
            byte[] encryptedData = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // Combine IV and encrypted data
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + encryptedData.length);
            byteBuffer.put(iv);
            byteBuffer.put(encryptedData);

            // Return as base64 string
            return Base64.getEncoder().encodeToString(byteBuffer.array());

        } catch (Exception e) {
            log.error("Encryption failed", e);
            throw new RuntimeException("Failed to encrypt data", e);
        }
    }

    /**
     * Decrypts the given base64-encoded ciphertext using AES-256-GCM
     *
     * @param base64Ciphertext Base64-encoded encrypted data with IV
     * @return Decrypted plaintext
     */
    public String decrypt(String base64Ciphertext) {
        try {
            // Decode from base64
            byte[] cipherMessage = Base64.getDecoder().decode(base64Ciphertext);

            // Extract IV and encrypted data
            ByteBuffer byteBuffer = ByteBuffer.wrap(cipherMessage);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);

            byte[] encryptedData = new byte[byteBuffer.remaining()];
            byteBuffer.get(encryptedData);

            // Initialize cipher for decryption
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            // Decrypt the data
            byte[] decryptedData = cipher.doFinal(encryptedData);

            return new String(decryptedData, StandardCharsets.UTF_8);

        } catch (Exception e) {
            log.error("Decryption failed", e);
            throw new RuntimeException("Failed to decrypt data", e);
        }
    }

    /**
     * Validates that the plaintext matches the encrypted value
     *
     * @param plaintext The plaintext to check
     * @param encryptedValue The encrypted value to compare against
     * @return true if they match, false otherwise
     */
    public boolean matches(String plaintext, String encryptedValue) {
        try {
            String decrypted = decrypt(encryptedValue);
            return plaintext.equals(decrypted);
        } catch (Exception e) {
            log.error("Failed to validate encrypted value", e);
            return false;
        }
    }

    /**
     * Generates a random encryption key for configuration
     * Use this method to generate a new key for your .env file
     *
     * @return Base64-encoded 256-bit key
     */
    public static String generateKey() {
        byte[] key = new byte[32]; // 256 bits
        new SecureRandom().nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }

    // For testing key generation
    public static void main(String[] args) {
        System.out.println("Generated AES-256 Key (add to .env file):");
        System.out.println("MPIN_ENCRYPTION_KEY=" + generateKey());
    }
}
