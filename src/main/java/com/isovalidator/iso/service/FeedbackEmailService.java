package com.isovalidator.iso.service;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import com.isovalidator.iso.DTO.FeedbackRequest;

@Service
public class FeedbackEmailService {

    private static final Logger log = LoggerFactory.getLogger(FeedbackEmailService.class);
    private static final String FEEDBACK_RECIPIENT = "paymentinfo1502@gmail.com";

    @Value("${FEEDBACK_SMTP_HOST:}")
    private String smtpHost;

    @Value("${FEEDBACK_SMTP_PORT:587}")
    private int smtpPort;

    @Value("${FEEDBACK_SMTP_USERNAME:}")
    private String smtpUsername;

    @Value("${FEEDBACK_SMTP_PASSWORD:}")
    private String smtpPassword;

    @Value("${FEEDBACK_SMTP_FROM:}")
    private String fromAddress;

    public boolean isConfigured() {
        return !smtpHost.isBlank()
                && !smtpUsername.isBlank()
                && !smtpPassword.isBlank()
                && !fromAddress.isBlank();
    }

    public void send(FeedbackRequest request) {
        log.info("Sending feedback email via SMTP host={} port={} usernameConfigured={} fromConfigured={}",
                smtpHost, smtpPort, !smtpUsername.isBlank(), !fromAddress.isBlank());
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(smtpHost);
        sender.setPort(smtpPort);
        sender.setUsername(smtpUsername);
        sender.setPassword(smtpPassword);

        Properties mailProperties = sender.getJavaMailProperties();
        mailProperties.put("mail.smtp.auth", "true");
        mailProperties.put("mail.smtp.starttls.enable", "true");
        mailProperties.put("mail.smtp.starttls.required", "true");
        mailProperties.put("mail.smtp.connectiontimeout", "5000");
        mailProperties.put("mail.smtp.timeout", "5000");
        mailProperties.put("mail.smtp.writetimeout", "5000");

        SimpleMailMessage email = new SimpleMailMessage();
        email.setFrom(fromAddress);
        email.setTo(FEEDBACK_RECIPIENT);
        email.setReplyTo(request.email().trim());
        email.setSubject("[FintechSchema] " + request.feedbackType().trim());
        email.setText(String.join("\n",
                "Feedback type: " + request.feedbackType().trim(),
                "Name: " + request.name().trim(),
                "Country: " + request.country().trim(),
                "Email: " + request.email().trim(),
                "",
                "Message:",
                request.message().trim()));
        try {
            sender.send(email);
            log.info("Feedback email sent successfully");
        } catch (org.springframework.mail.MailException exception) {
            log.error("Feedback email send failed: {}", exception.getMessage(), exception);
            throw exception;
        }
    }
}
