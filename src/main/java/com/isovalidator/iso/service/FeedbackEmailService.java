package com.isovalidator.iso.service;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.isovalidator.iso.DTO.FeedbackRequest;

@Service
public class FeedbackEmailService {

    private static final Logger log = LoggerFactory.getLogger(FeedbackEmailService.class);
    private static final String FEEDBACK_RECIPIENT = "paymentinfo1502@gmail.com";
    private static final String RESEND_EMAILS_URL = "https://api.resend.com/emails";

    @Value("${RESEND_API_KEY:}")
    private String resendApiKey;

    @Value("${RESEND_FROM_EMAIL:}")
    private String fromAddress;

    private final RestClient restClient = RestClient.create();

    public boolean isConfigured() {
        return !resendApiKey.isBlank() && !fromAddress.isBlank();
    }

    public void send(FeedbackRequest request) {
        log.info("Sending feedback email via Resend; apiKeyConfigured={} fromConfigured={}",
                !resendApiKey.isBlank(), !fromAddress.isBlank());

        String text = String.join("\n",
                "Feedback type: " + request.feedbackType().trim(),
                "Name: " + request.name().trim(),
                "Country: " + request.country().trim(),
                "Email: " + request.email().trim(),
                "",
                "Message:",
                request.message().trim());

        Map<String, Object> email = Map.of(
                "from", fromAddress,
                "to", List.of(FEEDBACK_RECIPIENT),
                "reply_to", request.email().trim(),
                "subject", "[FintechSchema] " + request.feedbackType().trim(),
                "text", text);

        try {
            restClient.post()
                    .uri(RESEND_EMAILS_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + resendApiKey)
                    .body(email)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Feedback email accepted by Resend");
        } catch (RestClientResponseException exception) {
            log.error("Resend rejected feedback email: status={} response={}",
                    exception.getStatusCode().value(), exception.getResponseBodyAsString());
            throw exception;
        } catch (ResourceAccessException exception) {
            log.error("Could not connect to the Resend email API", exception);
            throw exception;
        }
    }
}
