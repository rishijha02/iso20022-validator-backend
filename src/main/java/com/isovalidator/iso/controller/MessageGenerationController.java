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
import com.isovalidator.iso.service.GeneratorSchemaService;
import com.isovalidator.iso.DTO.GeneratorField;
import java.util.List;


@RestController
@RequestMapping("/v1/api/messages")
public class MessageGenerationController {

    private final MessageGenerationService
            messageGenerationService;
    private final GeneratorSchemaService generatorSchemaService;


    public MessageGenerationController(
            MessageGenerationService messageGenerationService,
            GeneratorSchemaService generatorSchemaService) {

        this.messageGenerationService =
                messageGenerationService;
        this.generatorSchemaService = generatorSchemaService;
    }

    @org.springframework.web.bind.annotation.GetMapping(value = "/types", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<String> getGeneratorMessageTypes(@RequestParam String family,
                                                 @RequestParam(defaultValue = "ISO") String geography) {
        return generatorSchemaService.messageTypes(family, geography);
    }

    @org.springframework.web.bind.annotation.GetMapping(value = "/versions", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<String> getGeneratorVersions(@RequestParam String geography,
                                             @RequestParam(defaultValue = "pacs.008") String messageType) {
        return generatorSchemaService.versions(messageType, geography);
    }

    @org.springframework.web.bind.annotation.GetMapping(value = "/schema", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<GeneratorField> getGeneratorSchema(
            @RequestParam String geography,
            @RequestParam(required = false) String version,
            @RequestParam(defaultValue = "pacs.008") String messageType) {
        return generatorSchemaService.fields(messageType, geography, version);
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
