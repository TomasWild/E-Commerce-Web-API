package com.wild.ecommerce.notification.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.base-url}")
    private String baseUrl;

    @Override
    @Async
    public void sendEmail(String to, String subject, String body) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();

            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");
            helper.setTo(to);
            helper.setFrom("noreply@ecommerce.com", "NoReply");
            helper.setSubject(subject);
            helper.setText(body, true);

            mailSender.send(mimeMessage);
        } catch (Exception e) {
            log.error("Failed to send email", e);
            throw new IllegalStateException("Failed to send email");
        }
    }

    @Override
    @Async
    public void sendVerificationEmail(String userName, String to, String token) {
        String verificationLink = baseUrl + "/api/v1/auth/verify?token=" + token;

        Context context = new Context();
        context.setVariable("userName", userName);
        context.setVariable("verificationLink", verificationLink);
        context.setVariable("expirationInMinutes", 15);

        String htmlBody = templateEngine.process("verification-email", context);
        sendEmail(to, "Verify your email", htmlBody);
    }
}
