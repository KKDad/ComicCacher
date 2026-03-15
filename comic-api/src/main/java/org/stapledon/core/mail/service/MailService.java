package org.stapledon.core.mail.service;

/**
 * Service for sending application emails (password resets, notifications, etc.).
 */
public interface MailService {

    /**
     * Sends a password reset email containing a reset token link.
     */
    void sendPasswordResetEmail(String email, String resetToken);
}
