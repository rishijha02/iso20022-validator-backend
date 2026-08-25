package com.isovalidator.iso.DTO;

import lombok.Data;

@Data
public class ValidationError {

    private String code;
    private String message;
    private int line;
    private int column;

    private String suggestion;

    private String technicalMessage;


}
