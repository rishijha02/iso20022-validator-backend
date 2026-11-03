package com.isovalidator.iso.DTO;

public record FeedbackRequest(
        String name,
        String country,
        String email,
        String feedbackType,
        String message,
        String website
) {}
