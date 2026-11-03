package com.isovalidator.iso.controller;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.isovalidator.iso.DTO.FeedbackRequest;
import com.isovalidator.iso.service.FeedbackEmailService;

@RestController
@RequestMapping("/v1/api/feedback")
public class FeedbackController {

    private static final Logger log = LoggerFactory.getLogger(FeedbackController.class);
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Set<String> FEEDBACK_TYPES = Set.of("Review", "Suggestion");

    private final FeedbackEmailService feedbackEmailService;

    public FeedbackController(FeedbackEmailService feedbackEmailService) {
        this.feedbackEmailService = feedbackEmailService;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> submit(@RequestBody FeedbackRequest request) {
        if (request == null) {
            return badRequest("Please complete the form.");
        }

        // Quietly accept bot submissions without sending email.
        if (request.website() != null && !request.website().isBlank()) {
            return ResponseEntity.ok(Map.of("message", "Thank you for your feedback."));
        }

        if (isBlankOrTooLong(request.name(), 120)
                || isBlankOrTooLong(request.country(), 100)
                || isBlankOrTooLong(request.email(), 254)
                || request.message() == null
                || request.message().trim().length() < 10
                || request.message().trim().length() > 5000
                || request.feedbackType() == null
                || !FEEDBACK_TYPES.contains(request.feedbackType().trim())
                || !EMAIL_PATTERN.matcher(request.email().trim()).matches()) {
            return badRequest("Please check the fields and try again. Your message must be between 10 and 5000 characters.");
        }

        if (!feedbackEmailService.isConfigured()) {
            log.error("Feedback email request rejected: SMTP configuration is incomplete");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("message", "Feedback email is not configured yet. Please try again later."));
        }

        try {
            feedbackEmailService.send(request);
            return ResponseEntity.ok(Map.of("message", "Thank you. Your feedback has been sent."));
        } catch (MailException | IllegalArgumentException exception) {
            log.error("Feedback email request failed: {}", exception.getMessage(), exception);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("message", "We could not send your feedback right now. Please try again later."));
        }
    }

    private static boolean isBlankOrTooLong(String value, int maxLength) {
        return value == null || value.isBlank() || value.trim().length() > maxLength;
    }

    private static ResponseEntity<Map<String, String>> badRequest(String message) {
        return ResponseEntity.badRequest().body(Map.of("message", message));
    }
}
