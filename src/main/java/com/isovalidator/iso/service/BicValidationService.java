package com.isovalidator.iso.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.isovalidator.iso.DTO.IdentifierValidationResponse;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class BicValidationService {

    private static final String BIC_REGEX =
            "^[A-Z]{4}[A-Z]{2}[A-Z0-9]{2}([A-Z0-9]{3})?$";

            public IdentifierValidationResponse validate(
            String bicInput
    ) {

        log.info("ENtering Service layer for validating bic " + bicInput);
        IdentifierValidationResponse response =
                new IdentifierValidationResponse();

        response.setType("BIC");

        if (bicInput == null ||
                bicInput.isBlank()) {

            response.setValid(false);

            response.setMessage(
                    "Please enter a BIC."
            );

            return response;
        }

        String bic =
                bicInput
                        .replaceAll("\\s+", "")
                        .toUpperCase();

        response.setValue(bic);


        if (!bic.matches(BIC_REGEX)) {

            response.setValid(false);

            response.setMessage(
                    "Invalid BIC format. A BIC must contain 8 or 11 characters."
            );

            return response;
        }

        String institutionCode =
                bic.substring(0, 4);

        String countryCode =
                bic.substring(4, 6);

        String locationCode =
                bic.substring(6, 8);

        String branchCode =
                bic.length() == 11
                        ? bic.substring(8, 11)
                        : "XXX";


        Map<String, String> details =
                new HashMap<>();

        details.put(
                "Institution Code",
                institutionCode
        );

        details.put(
                "Country Code",
                countryCode
        );

        details.put(
                "Location Code",
                locationCode
        );

        details.put(
                "Branch Code",
                branchCode
        );


        response.setDetails(details);

        response.setValid(true);

        response.setMessage(
                "The BIC format is valid."
        );

        return response;
    }

}
