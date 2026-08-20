package com.isovalidator.iso.service;

import com.isovalidator.iso.DTO.MessageSummaryResponse;

public interface MessageSummaryService {
    MessageSummaryResponse generateSummary(String xml);

    
} 