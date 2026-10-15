package com.isovalidator.iso.DTO.OpenBanking;

import java.math.BigDecimal;

import lombok.Data;

@Data 
public class AccountResponse {

    private String AccNum;
    private String AccType;
    private BigDecimal balance;
    private String currency;

}
