package com.bank.datamodel.service;

import java.math.BigDecimal;

/** 信用卡業務介面。 */
public interface CreditCardService {

    void chargeCreditCard(String cardNo, BigDecimal amount);

    void payCreditCardBill(String cardNo, String twdAccountNo, BigDecimal amount);
}
