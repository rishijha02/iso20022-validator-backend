package com.isovalidator.iso.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ValidationErrorTranslator {

    public static FriendlyError translate(String technicalMessage) {

        if (technicalMessage == null) {
            return new FriendlyError(
                    "Validation error",
                    "The XML message does not match the expected ISO 20022 structure."
            );
        }

        /*
         * Invalid element order
         */
        if (technicalMessage.contains("Invalid content was found starting with element")) {

            String invalidElement =
                    extractElement(
                            technicalMessage,
                            "starting with element"
                    );

            String expectedElements =
                    extractExpectedElements(technicalMessage);

            String message;

            if (invalidElement != null) {

                message = invalidElement +
                        " is in the wrong position";

            } else {

                message =
                        "An element is in the wrong position";

            }

            String suggestion;

            if (expectedElements != null) {

                suggestion =
                        "Check the element order. Expected: " +
                        expectedElements +
                        ".";

            } else {

                suggestion =
                        "Check the XML element order against the expected message schema.";

            }

            return new FriendlyError(
                    message,
                    suggestion
            );
        }


        /*
         * Missing required element
         */
        if (technicalMessage.contains("is expected")) {

            return new FriendlyError(
                    "A required element is missing",
                    "Add the required element at the expected location in the XML message."
            );
        }


        /*
         * Invalid value
         */
        if (technicalMessage.contains("is not a valid value")
                || technicalMessage.contains("not facet-valid")) {

            return new FriendlyError(
                    "Invalid value",
                    "The value does not match the format required by the ISO 20022 schema."
            );
        }


        /*
         * Invalid data type
         */
        if (technicalMessage.contains("not a valid value for")) {

            return new FriendlyError(
                    "Invalid data format",
                    "Check that the value has the correct format."
            );
        }


        /*
         * Default
         */
        return new FriendlyError(
                "XML validation failed",
                "The message does not match the expected XML schema. Review the highlighted location."
        );
    }


    private static String extractElement(
            String message,
            String prefix
    ) {

        Pattern pattern =
                Pattern.compile(
                        "element.*?:(\\w+)"
                );

        Matcher matcher =
                pattern.matcher(message);

        if (matcher.find()) {

            return matcher.group(1);

        }

        return null;
    }


    private static String extractExpectedElements(
            String message
    ) {

        Pattern pattern =
                Pattern.compile(
                        "One of '(.+?)' is expected"
                );

        Matcher matcher =
                pattern.matcher(message);

        if (matcher.find()) {

            String result =
                    matcher.group(1);

            return result
                    .replaceAll(
                            "\\{[^}]+\\}:",
                            ""
                    );

        }

        return null;
    }
}