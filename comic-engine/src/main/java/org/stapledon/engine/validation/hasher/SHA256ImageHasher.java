package org.stapledon.engine.validation.hasher;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


import org.stapledon.common.service.ImageHasher;

/**
 * SHA-256 hash implementation for duplicate image detection.
 * More secure than MD5, byte-exact matching only. Slower but more collision-resistant.
 * Won't catch re-encoded images with different byte representations.
 */
@Slf4j
@ToString
@Component("sha256ImageHasher")
public class SHA256ImageHasher implements ImageHasher {

    @Override
    public String calculateHash(byte[] imageData) {
        if (imageData == null || imageData.length == 0) {
            log.warn("Cannot calculate SHA-256 hash for null or empty image data");
            return null;
        }

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] messageDigest = md.digest(imageData);
            BigInteger no = new BigInteger(1, messageDigest);
            StringBuilder hashtext = new StringBuilder(no.toString(16));

            // Pad with leading zeros to 64 characters
            while (hashtext.length() < 64) {
                hashtext.insert(0, "0");
            }

            return hashtext.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not available: {}", e.getMessage(), e);
            return null;
        }
    }
}
