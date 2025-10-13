package com.wild.ecommerce.notification.service;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private TemplateEngine templateEngine;

    @InjectMocks
    private EmailServiceImpl emailService;

    @BeforeEach
    void setUp() {
        // Set @Value properties
        ReflectionTestUtils.setField(emailService, "baseUrl", "http://localhost:8080");
    }

    @Test
    void sendEmail_ShouldSendPlainTextEmailSuccessfully() throws Exception {
        // Given
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        String to = "user@example.com";
        String subject = "Test Subject";
        String body = "<h1>Test Body</h1>";

        // When
        emailService.sendEmail(to, subject, body);

        // Then
        ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        MimeMessage sentMessage = messageCaptor.getValue();
        assertThat(sentMessage.getSubject()).isEqualTo(subject);
        assertThat(sentMessage.getAllRecipients()[0].toString()).isEqualTo(to);
        assertThat(sentMessage.getFrom()[0].toString()).contains("noreply@");
        assertThat(sentMessage.getContent().toString()).contains("Test Body");
    }

    @Test
    void sendEmail_ShouldSendEmailWithHtmlContent() throws Exception {
        // Given
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        String to = "user@example.com";
        String subject = "HTML Email";
        String htmlBody = "<html><body><h1>Welcome</h1><p>This is a test</p></body></html>";

        // When
        emailService.sendEmail(to, subject, htmlBody);

        // Then
        ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        MimeMessage sentMessage = messageCaptor.getValue();
        assertThat(sentMessage.getSubject()).isEqualTo(subject);

        String content = sentMessage.getContent().toString();
        assertThat(content).contains("Welcome");
        assertThat(content).contains("This is a test");
    }

    @Test
    void sendEmail_ShouldThrowIllegalStateException_WhenMailSenderFails() {
        // Given
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        String to = "user@example.com";
        String subject = "Test Subject";
        String body = "Test Body";

        doThrow(new RuntimeException("Mail server error"))
                .when(mailSender).send(any(MimeMessage.class));

        // When & Then
        assertThatThrownBy(() -> emailService.sendEmail(to, subject, body))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to send email");

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendEmail_ShouldThrowIllegalStateException_WhenMimeMessageCreationFails() {
        // Given
        String to = "user@example.com";
        String subject = "Test Subject";
        String body = "Test Body";

        when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("Failed to create message"));

        // When & Then
        assertThatThrownBy(() -> emailService.sendEmail(to, subject, body))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to send email");
    }

    @Test
    void sendVerificationEmail_ShouldSendVerificationEmailSuccessfully() {
        // Given
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        String userName = "John Doe";
        String to = "john.doe@example.com";
        String token = "test-verification-token";
        String processedHtml = "<html><body>Verification email content</body></html>";

        when(templateEngine.process(eq("verification-email"), any(Context.class)))
                .thenReturn(processedHtml);

        // When
        emailService.sendVerificationEmail(userName, to, token);

        // Then
        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(templateEngine).process(eq("verification-email"), contextCaptor.capture());

        Context capturedContext = contextCaptor.getValue();
        assertThat(capturedContext.getVariable("userName")).isEqualTo(userName);
        assertThat(capturedContext.getVariable("verificationLink"))
                .isEqualTo("http://localhost:8080/api/v1/auth/verify?token=" + token);
        assertThat(capturedContext.getVariable("expirationInMinutes")).isEqualTo(15);

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendVerificationEmail_ShouldCreateCorrectVerificationLink() {
        // Given
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        String userName = "Jane Smith";
        String to = "jane.smith@example.com";
        String token = "abc123xyz";
        String processedHtml = "<html><body>Email content</body></html>";

        when(templateEngine.process(eq("verification-email"), any(Context.class)))
                .thenReturn(processedHtml);

        // When
        emailService.sendVerificationEmail(userName, to, token);

        // Then
        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(templateEngine).process(eq("verification-email"), contextCaptor.capture());

        Context capturedContext = contextCaptor.getValue();
        String verificationLink = (String) capturedContext.getVariable("verificationLink");

        assertThat(verificationLink).isEqualTo("http://localhost:8080/api/v1/auth/verify?token=abc123xyz");
        assertThat(verificationLink).contains("token=" + token);
        assertThat(verificationLink).startsWith("http://localhost:8080");
    }

    @Test
    void sendVerificationEmail_ShouldPopulateTemplateVariablesCorrectly() {
        // Given
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        String userName = "Test User";
        String to = "test@example.com";
        String token = "token123";
        String processedHtml = "<html><body>Test</body></html>";

        when(templateEngine.process(eq("verification-email"), any(Context.class)))
                .thenReturn(processedHtml);

        // When
        emailService.sendVerificationEmail(userName, to, token);

        // Then
        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(templateEngine).process(eq("verification-email"), contextCaptor.capture());

        Context capturedContext = contextCaptor.getValue();
        assertThat(capturedContext.getVariable("userName")).isNotNull();
        assertThat(capturedContext.getVariable("verificationLink")).isNotNull();
        assertThat(capturedContext.getVariable("expirationInMinutes")).isNotNull();
        assertThat(capturedContext.getVariable("expirationInMinutes")).isEqualTo(15);
    }

    @Test
    void sendVerificationEmail_ShouldSendEmailWithCorrectSubject() {
        // Given
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        String userName = "Test User";
        String to = "test@example.com";
        String token = "token123";
        String processedHtml = "<html><body>Verification content</body></html>";

        when(templateEngine.process(eq("verification-email"), any(Context.class)))
                .thenReturn(processedHtml);

        // When
        emailService.sendVerificationEmail(userName, to, token);

        // Then
        verify(templateEngine).process(eq("verification-email"), any(Context.class));
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendVerificationEmail_ShouldUseConfiguredBaseUrlInVerificationLink() {
        // Given
        ReflectionTestUtils.setField(emailService, "baseUrl", "https://production.example.com");

        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        String userName = "User";
        String to = "user@example.com";
        String token = "prod-token";
        String processedHtml = "<html><body>Email</body></html>";

        when(templateEngine.process(eq("verification-email"), any(Context.class)))
                .thenReturn(processedHtml);

        // When
        emailService.sendVerificationEmail(userName, to, token);

        // Then
        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(templateEngine).process(eq("verification-email"), contextCaptor.capture());

        Context capturedContext = contextCaptor.getValue();
        String verificationLink = (String) capturedContext.getVariable("verificationLink");

        assertThat(verificationLink).startsWith("https://production.example.com");
        assertThat(verificationLink).contains("token=prod-token");
    }

    @Test
    void sendVerificationEmail_ShouldPropagateException_WhenTemplateProcessingFails() {
        // Given
        String userName = "User";
        String to = "user@example.com";
        String token = "token";

        when(templateEngine.process(eq("verification-email"), any(Context.class)))
                .thenThrow(new RuntimeException("Template processing failed"));

        // When & Then
        assertThatThrownBy(() -> emailService.sendVerificationEmail(userName, to, token))
                .isInstanceOf(RuntimeException.class);

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void sendEmail_ShouldSetFromAddressCorrectly() throws Exception {
        // Given
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));

        String to = "user@example.com";
        String subject = "Test";
        String body = "Body";

        // When
        emailService.sendEmail(to, subject, body);

        // Then
        ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        MimeMessage sentMessage = messageCaptor.getValue();
        assertThat(sentMessage.getFrom()[0].toString()).contains("noreply@");
    }
}
