package com.isovalidator.iso.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.isovalidator.iso.DTO.IdentifierValidationResponse;

@Service
public class IbanValidationService {


    private static final Map<String, Integer> IBAN_LENGTHS = Map.ofEntries(

            Map.entry("DE", 22),
            Map.entry("FR", 27),
            Map.entry("GB", 22),
            Map.entry("ES", 24),
            Map.entry("IT", 27),
            Map.entry("NL", 18),
            Map.entry("BE", 16),
            Map.entry("CH", 21),
            Map.entry("AT", 20),
            Map.entry("SE", 24),
            Map.entry("NO", 15),
            Map.entry("DK", 18),
            Map.entry("FI", 18),
            Map.entry("PL", 28),
            Map.entry("IE", 22),
            Map.entry("PT", 25),
            Map.entry("IL", 23)
    );

    public IdentifierValidationResponse validate(String ibanInput) {

        IdentifierValidationResponse response =
                new IdentifierValidationResponse();

        response.setType("IBAN");

        if (ibanInput == null || ibanInput.isBlank()) {

            response.setValid(false);
            response.setMessage("Please enter an IBAN.");

            return response;
        }

        String iban = ibanInput
                .replaceAll("\\s+", "")
                .toUpperCase();

        response.setValue(iban);

        if (!iban.matches("^[A-Z0-9]+$")) {

            response.setValid(false);
            response.setMessage(
                    "IBAN can contain only letters and numbers."
            );

            return response;
        }

        if (iban.length() < 4) {

            response.setValid(false);
            response.setMessage(
                    "The IBAN is too short."
            );

            return response;
        }

        String countryCode = iban.substring(0, 2);

        Integer expectedLength =
                IBAN_LENGTHS.get(countryCode);

        if (expectedLength == null) {

            response.setValid(false);
            response.setMessage(
                    "This country code is not currently supported."
            );

            return response;
        }

        if (iban.length() != expectedLength) {

            response.setValid(false);

            response.setMessage(
                    "Invalid IBAN length. Expected "
                            + expectedLength
                            + " characters for "
                            + countryCode
                            + "."
            );

            return response;
        }

        boolean checksumValid =
                isChecksumValid(iban);

        if (!checksumValid) {

            response.setValid(false);

            response.setMessage(
                    "The IBAN checksum is invalid."
            );

            return response;
        }

        Map<String, String> details =
                new HashMap<>();

        details.put(
                "Country Code",
                countryCode
        );

        details.put(
                "Expected Length",
                String.valueOf(expectedLength)
        );

        response.setDetails(details);

        response.setValid(true);

        response.setMessage(
                "The IBAN is valid."
        );

        return response;
    }


    private boolean isChecksumValid(String iban) {

        String rearranged =
                iban.substring(4)
                        + iban.substring(0, 4);

        StringBuilder numericIban =
                new StringBuilder();

        for (char character :
                rearranged.toCharArray()) {

            if (Character.isDigit(character)) {

                numericIban.append(character);

            } else {

                int value =
                        Character.getNumericValue(
                                character
                        );

                numericIban.append(value);
            }
        }

        return mod97(
                numericIban.toString()
        ) == 1;
    }


    private int mod97(String value) {

        int remainder = 0;

        for (char character :
                value.toCharArray()) {

            int digit =
                    character - '0';

            remainder =
                    (remainder * 10 + digit)
                            % 97;
        }

        return remainder;
    }

}
