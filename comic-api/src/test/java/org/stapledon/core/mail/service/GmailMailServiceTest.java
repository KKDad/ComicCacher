package org.stapledon.core.mail.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

class GmailMailServiceTest {

    @Test
    void smtpFailureIsLoggedNotThrown() {
        JavaMailSender sender = mock(JavaMailSender.class);
        doThrow(new MailSendException("SMTP down")).when(sender).send(any(SimpleMailMessage.class));
        GmailMailService service = new GmailMailService(sender);

        // Throwing only for known addresses would let callers tell which emails have accounts
        assertThatCode(() -> service.sendPasswordResetEmail("alice@example.com", "token")).doesNotThrowAnyException();
        verify(sender).send(any(SimpleMailMessage.class));
    }

    @Test
    void missingMailSenderDoesNotThrow() {
        assertThatCode(() -> new GmailMailService(null).sendPasswordResetEmail("alice@example.com", "token")).doesNotThrowAnyException();
    }
}
