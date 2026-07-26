package com.bank.datamodel.service;

import java.math.BigDecimal;

/** 換匯業務介面。 */
public interface FxService {

    void buyForeignCurrency(String twdAccountNo, String fxAccountNo,
                            BigDecimal twdAmount, BigDecimal exchangeRate);
}
