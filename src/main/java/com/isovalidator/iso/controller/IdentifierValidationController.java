package com.isovalidator.iso.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.isovalidator.iso.DTO.IdentifierValidationRequest;
import com.isovalidator.iso.DTO.IdentifierValidationResponse;
import com.isovalidator.iso.service.BicValidationService;
import com.isovalidator.iso.service.IbanValidationService;

@RestController
@RequestMapping("/v1/api")
public class IdentifierValidationController {

    private final IbanValidationService ibanValidationService;

    private final BicValidationService bicValidationService;


    public IdentifierValidationController(
            IbanValidationService ibanValidationService,
            BicValidationService bicValidationService
    ) {

        this.ibanValidationService =
                ibanValidationService;

        this.bicValidationService =
                bicValidationService;
    }


    @PostMapping("/iban/validate")
    public ResponseEntity<IdentifierValidationResponse>
    validateIban(
            @RequestBody
            IdentifierValidationRequest request
    ) {

        return ResponseEntity.ok(
                ibanValidationService.validate(
                        request.getValue()
                )
        );
    }


    @PostMapping("/bic/validate")
    public ResponseEntity<IdentifierValidationResponse>
    validateBic(
            @RequestBody
            IdentifierValidationRequest request
    ) {

        return ResponseEntity.ok(
                bicValidationService.validate(
                        request.getValue()
                )
        );
    }

}
