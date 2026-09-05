package com.isovalidator.iso.service;

import com.isovalidator.iso.DTO.MessageGenerationRequest;
import com.isovalidator.iso.DTO.MessageGenerationResponse;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class MessageGenerationService {

    private final Map<String, MessageGenerator> generators;

    public MessageGenerationService(
            List<MessageGenerator> generatorList) {

        this.generators =
                generatorList.stream()
                        .collect(
                                Collectors.toMap(
                                        generator ->
                                                generator
                                                        .getMessageType()
                                                        .toLowerCase(),

                                        Function.identity()
                                )
                        );
    }


    public MessageGenerationResponse generate(
            MessageGenerationRequest request,
            String geography) {

        MessageGenerationResponse response =
                new MessageGenerationResponse();

        try {

            if (request == null) {
                throw new IllegalArgumentException(
                        "Generation request cannot be null"
                );
            }

            if (request.getMessageType() == null ||
                    request.getMessageType().isBlank()) {

                throw new IllegalArgumentException(
                        "Message type is required"
                );
            }

            String messageType =
                    request.getMessageType()
                            .trim()
                            .toLowerCase();

            MessageGenerator generator =
                    generators.get(messageType);

            if (generator == null) {

                throw new IllegalArgumentException(
                        "Unsupported message type: "
                                + request.getMessageType()
                );
            }

            return generator.generate(
                    request,
                    geography
            );

        } catch (Exception e) {

            response.setSuccess(false);
            response.setMessageType(
                    request != null
                            ? request.getMessageType()
                            : null
            );
            response.setGeography(geography);
            response.setError(e.getMessage());

            return response;
        }
    }
}