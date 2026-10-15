package com.isovalidator.iso.DTO.OpenBanking;

import java.math.BigDecimal;
import java.sql.Date;

import lombok.Data;

@Data 
public class TransactionReponse {

    private String TransactionId;
    private String description;
    private String currency;
    private BigDecimal amount;
    private Date bookingdate;

}
