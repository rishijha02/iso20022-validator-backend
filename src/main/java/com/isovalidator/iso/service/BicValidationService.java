package com.isovalidator.iso.service;

import com.isovalidator.iso.DTO.IdentifierValidationResponse;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
public class BicValidationService {

    /*
     * ISO 3166-1 Alpha-2 country codes.
     *
     * This is used for Level 2 validation:
     * BIC country code must represent a real country.
     */
    private static final Set<String> COUNTRY_CODES = Set.of(

            "AF", "AL", "DZ", "AS", "AD", "AO", "AI", "AQ",
            "AG", "AR", "AM", "AW", "AU", "AT", "AZ",

            "BS", "BH", "BD", "BB", "BY", "BE", "BZ", "BJ",
            "BM", "BT", "BO", "BQ", "BA", "BW", "BV", "BR",
            "IO", "BN", "BG", "BF", "BI",

            "CV", "KH", "CM", "CA", "KY", "CF", "TD", "CL",
            "CN", "CX", "CC", "CO", "KM", "CG", "CD", "CK",
            "CR", "CI", "HR", "CU", "CW", "CY", "CZ",

            "DK", "DJ", "DM", "DO", "EC", "EG", "SV", "GQ",
            "ER", "EE", "SZ", "ET",

            "FK", "FO", "FJ", "FI", "FR", "GF", "PF", "TF",
            "GA", "GM", "GE", "DE", "GH", "GI", "GR", "GL",
            "GD", "GP", "GU", "GT", "GG", "GN", "GW", "GY",

            "HT", "HM", "VA", "HN", "HK", "HU",

            "IS", "IN", "ID", "IR", "IQ", "IE", "IM", "IL",
            "IT",

            "JM", "JP", "JE", "JO",

            "KZ", "KE", "KI", "KP", "KR", "KW", "KG",

            "LA", "LV", "LB", "LS", "LR", "LY", "LI", "LT",
            "LU",

            "MO", "MG", "MW", "MY", "MV", "ML", "MT", "MH",
            "MQ", "MR", "MU", "YT", "MX", "FM", "MD", "MC",
            "MN", "ME", "MS", "MA", "MZ", "MM",

            "NA", "NR", "NP", "NL", "NC", "NZ", "NI", "NE",
            "NG", "NU", "NF", "MK", "MP", "NO",

            "OM",

            "PK", "PW", "PS", "PA", "PG", "PY", "PE", "PH",
            "PN", "PL", "PT", "PR",

            "QA",

            "RE", "RO", "RU", "RW",

            "BL", "SH", "KN", "LC", "MF", "PM", "VC", "WS",
            "SM", "ST", "SA", "SN", "RS", "SC", "SL", "SG",
            "SX", "SK", "SI", "SB", "SO", "ZA", "GS", "SS",
            "ES", "LK", "SD", "SR", "SJ", "SE", "CH", "SY",
            "TW", "TJ", "TZ", "TH", "TL", "TG", "TK", "TO",
            "TT", "TN", "TR", "TM", "TC", "TV",

            "UG", "UA", "AE", "GB", "US", "UM", "UY", "UZ",

            "VU", "VE", "VN", "VG", "VI",

            "WF", "EH", "YE", "ZM", "ZW"
    );


    public IdentifierValidationResponse validate(String value) {

        IdentifierValidationResponse response =
                new IdentifierValidationResponse();

        response.setType("BIC");
        response.setValue(value);


        /*
         * ------------------------------------------
         * Basic input validation
         * ------------------------------------------
         */

        if (value == null || value.trim().isEmpty()) {

            response.setValid(false);

            response.setMessage(
                    "Please enter a BIC."
            );

            return response;
        }


        /*
         * Remove spaces and convert to uppercase.
         */

        String bic = value
                .trim()
                .replaceAll("\\s+", "")
                .toUpperCase();


        /*
         * ------------------------------------------
         * LEVEL 1
         * Structural validation
         * ------------------------------------------
         */

        if (!(bic.length() == 8 || bic.length() == 11)) {

            response.setValid(false);

            response.setMessage(
                    "Invalid BIC: BIC must contain 8 or 11 characters."
            );

            return response;
        }


        /*
         * Bank code
         *
         * Characters 1-4
         * Must contain letters only.
         */

        String bankCode = bic.substring(0, 4);

        if (!bankCode.matches("[A-Z]{4}")) {

            response.setValid(false);

            response.setMessage(
                    "Invalid BIC: bank code must contain 4 letters."
            );

            return response;
        }


        /*
         * Country code
         *
         * Characters 5-6
         */

        String countryCode = bic.substring(4, 6);


        /*
         * Location code
         *
         * Characters 7-8
         *
         * SWIFT BIC allows letters or numbers here.
         */

        String locationCode = bic.substring(6, 8);

        if (!locationCode.matches("[A-Z0-9]{2}")) {

            response.setValid(false);

            response.setMessage(
                    "Invalid BIC: location code must contain 2 letters or numbers."
            );

            return response;
        }


        /*
         * Branch code
         *
         * Characters 9-11
         *
         * Optional for 8-character BIC.
         */

        String branchCode = null;

        if (bic.length() == 11) {

            branchCode = bic.substring(8, 11);

            if (!branchCode.matches("[A-Z0-9]{3}")) {

                response.setValid(false);

                response.setMessage(
                        "Invalid BIC: branch code must contain 3 letters or numbers."
                );

                return response;
            }
        }


        /*
         * ------------------------------------------
         * LEVEL 2
         * Country validation
         * ------------------------------------------
         */

        if (!COUNTRY_CODES.contains(countryCode)) {

            response.setValid(false);

            response.setMessage(
                    "Invalid BIC: '" +
                    countryCode +
                    "' is not a recognized ISO country code."
            );


            Map<String, String> details =
                    new HashMap<>();

            details.put("bankCode", bankCode);
            details.put("countryCode", countryCode);
            details.put("locationCode", locationCode);

            if (branchCode != null) {
                details.put("branchCode", branchCode);
            }

            response.setDetails(details);

            return response;
        }


        /*
         * ------------------------------------------
         * Valid BIC
         * ------------------------------------------
         */

        response.setValid(true);

        response.setMessage(
                "Valid BIC format and country code."
        );


        Map<String, String> details =
                new HashMap<>();

        details.put("bankCode", bankCode);
        details.put("countryCode", countryCode);
        details.put(
                "country",
                getCountryName(countryCode)
        );
        details.put("locationCode", locationCode);

        if (branchCode != null) {

            details.put(
                    "branchCode",
                    branchCode
            );
        }


        response.setDetails(details);

        return response;
    }


    /*
     * Converts commonly used country codes
     * into readable country names.
     *
     * You can expand this later or replace it
     * with a proper ISO country library.
     */
    private String getCountryName(String code) {

        return switch (code) {

            case "DE" -> "Germany";
            case "FR" -> "France";
            case "GB" -> "United Kingdom";
            case "US" -> "United States";
            case "IN" -> "India";
            case "IL" -> "Israel";
            case "CH" -> "Switzerland";
            case "NL" -> "Netherlands";
            case "BE" -> "Belgium";
            case "ES" -> "Spain";
            case "IT" -> "Italy";
            case "AT" -> "Austria";
            case "AU" -> "Australia";
            case "CA" -> "Canada";
            case "SG" -> "Singapore";
            case "JP" -> "Japan";
            case "CN" -> "China";
            case "AE" -> "United Arab Emirates";
            case "SA" -> "Saudi Arabia";
            case "IE" -> "Ireland";
            case "SE" -> "Sweden";
            case "NO" -> "Norway";
            case "DK" -> "Denmark";
            case "FI" -> "Finland";
            case "PL" -> "Poland";
            case "PT" -> "Portugal";
            case "GR" -> "Greece";
            case "CZ" -> "Czech Republic";
            case "HU" -> "Hungary";
            case "RO" -> "Romania";
            case "BG" -> "Bulgaria";
            case "ZA" -> "South Africa";
            case "NZ" -> "New Zealand";
            case "BR" -> "Brazil";
            case "MX" -> "Mexico";

            default -> code;
        };
    }
}