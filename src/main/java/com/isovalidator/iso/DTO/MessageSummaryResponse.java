package com.isovalidator.iso.DTO;

import java.util.LinkedHashMap;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageSummaryResponse {

    private String messageType;

    private boolean success;

    private String version;

    private String title;

  //  private String messageId;
    private String errorMessage;

   // private String creationDateTime;

   // private Integer numberOfTransactions;
    private Map<String, Object> summary = new LinkedHashMap<>();;

    //private List<TransactionSummary> transactions;

}
