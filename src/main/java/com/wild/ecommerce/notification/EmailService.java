package com.wild.ecommerce.notification;

public interface EmailService {

    void sendEmail(String to, String subject, String body);

    void sendVerificationEmail(String userName, String to, String token);
}
