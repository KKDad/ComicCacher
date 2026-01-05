package org.stapledon.engine.validation.hasher;

import org.springframework.stereotype.Component;
import org.stapledon.common.service.ImageHasher;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * MD5 hash implementation for duplicate image detection.
 * Fast, byte-exact matching only. Good for detecting exact duplicates
 * but won't catch re-encoded images with different byte representations.
 */
@Slf4j
@ToString
@Component("md5ImageHasher")
public class MD5ImageHasher implements ImageHasher {

    @Override
    public String calculateHash(byte[] imageData) {
        if (imageData == null || imageData.length == 0) {
            log.warn("Cannot calculate MD5 hash for null or empty image data");
            return null;
        }

        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(imageData);
            BigInteger no = new BigInteger(1, messageDigest);
            String hashtext = no.toString(16);

            // Pad with leading zeros to 32 characters
            while (hashtext.length() < 32) {
                hashtext = "0" + hashtext;
            }

            return hashtext;
        } catch (NoSuchAlgorithmException e) {
            log.error("MD5 algorithm not available: {}", e.getMessage(), e);
            return null;
        }
    }
}
