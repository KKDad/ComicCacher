package org.stapledon.core.mail.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * SMTP-based mail service implementation.
 * Gracefully degrades when JavaMailSender is not configured (e.g., in tests).
 */
@Slf4j
@Service
public class GmailMailService implements MailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:noreply@comiccacher.local}")
    private String fromAddress;

    @Value("${app.mail.reset-url-base:http://localhost:3000/reset-password}")
    private String resetUrlBase;

    public GmailMailService(@Nullable JavaMailSender mailSender) {
        this.mailSender = mailSender;
        if (mailSender == null) {
            log.warn("JavaMailSender not configured — password reset emails will be logged only");
        }
    }

    @Override
    public void sendPasswordResetEmail(String email, String resetToken) {
        if (mailSender == null) {
            log.warn("Mail not configured, password reset requested for: {}", email);
            return;
        }

        String resetLink = resetUrlBase + "?token=" + resetToken;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(email);
        message.setSubject("Password Reset Request");
        message.setText("You requested a password reset.\n\n"
                + "Click the link below to reset your password:\n"
                + resetLink + "\n\n"
                + "This link expires in 15 minutes.\n\n"
                + "If you did not request this, please ignore this email.");

        mailSender.send(message);
        log.info("Password reset email sent to: {}", email);
    }
}
