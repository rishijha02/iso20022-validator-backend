package com.isovalidator.iso.service;

import com.isovalidator.iso.DTO.MessageGenerationRequest;
import com.isovalidator.iso.DTO.MessageGenerationResponse;

public interface MessageGenerator {

     String getMessageType();

    MessageGenerationResponse generate(
            MessageGenerationRequest request,
            String geography
    );

}
