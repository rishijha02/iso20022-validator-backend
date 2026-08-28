package com.isovalidator.iso.DTO;

import java.util.Map;

import lombok.Data;

@Data
public class IdentifierValidationResponse {

    private boolean valid;

    private String type;

    private String value;

    private String message;

    private Map<String, String> details;

}
