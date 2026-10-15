package com.isovalidator.iso.service.OpenBanking;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.isovalidator.iso.DTO.OpenBanking.AccountResponse;
import com.isovalidator.iso.DTO.OpenBanking.ConsentRequest;
import com.isovalidator.iso.DTO.OpenBanking.ConsentResponse;
import com.isovalidator.iso.DTO.OpenBanking.ConsentStatus;
import com.isovalidator.iso.DTO.OpenBanking.TransactionReponse;

@Service 
public class Openbankingservice {

    public ConsentResponse createconsent(ConsentRequest consentrequest)
    {
        ConsentResponse consentResponse= new ConsentResponse();
        Random random=new Random();
        int number = random.nextInt(1000000); 
        String formattedString = String.format("%06d", number);
        String consentId="";
        consentId="CONS_"+formattedString;

        consentResponse.setConsentId(consentId);
        consentResponse.setPermission(consentrequest.getPermission());
        consentResponse.setStatus(ConsentStatus.AWAITING_AUTHORIZATION);
        
        return consentResponse;
    }

    public ConsentResponse authoriseconsent(String consentId,ConsentRequest consentRequest)
    {
        ConsentResponse consentResponse = new ConsentResponse();

        consentResponse.setConsentId(consentId);
        consentResponse.setMessage("customer authorised the consent");
        consentResponse.setStatus(ConsentStatus.AUTHORIZED);

        return consentResponse;
    }

    public ConsentResponse getaccesstoken (String consentId, ConsentRequest consentRequest)
    {
        ConsentResponse consentResponse = new ConsentResponse();

        String uniqueAccessToken = UUID.randomUUID().toString();

        consentResponse.setAccess_token(uniqueAccessToken);
        consentResponse.setExpiresin(new BigDecimal(3600));
        consentResponse.setTokenType("Bearer");
       // consentResponse.setConsentId();
       return  consentResponse;
    }

    public List<AccountResponse> getaccount (String  consentId)
    {
        List<AccountResponse> accounts = new ArrayList<>();

        return accounts;
    }

    public List<TransactionReponse> getTransactions (String accountid,ConsentRequest consentRequest)
    {
        List<TransactionReponse> transactionReponses = new ArrayList<>();

        return transactionReponses;
    }

}
