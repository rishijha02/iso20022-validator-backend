package com.isovalidator.iso.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.isovalidator.iso.DTO.MessageGenerationRequest;
import com.isovalidator.iso.DTO.MessageGenerationResponse;
import com.isovalidator.iso.service.MessageGenerationService;


@RestController
@RequestMapping("/v1/api/messages")
public class MessageGenerationController {

    private final MessageGenerationService
            messageGenerationService;


    public MessageGenerationController(
            MessageGenerationService messageGenerationService) {

        this.messageGenerationService =
                messageGenerationService;
    }


    @PostMapping(
            value = "/generate",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
            
    )
    public ResponseEntity<MessageGenerationResponse> generateMessage(

            @RequestParam String geography,

            @RequestBody
            MessageGenerationRequest request) {


        MessageGenerationResponse response =
                messageGenerationService.generate(
                        request,
                        geography
                );


        return ResponseEntity.ok(response);
    }

}
