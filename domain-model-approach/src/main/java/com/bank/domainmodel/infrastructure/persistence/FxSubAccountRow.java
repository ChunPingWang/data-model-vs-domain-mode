package com.bank.domainmodel.infrastructure.persistence;

import java.math.BigDecimal;

/**
 * FX_SUB_ACCOUNT 幣別子帳明細表的一列——每個幣別一列。
 *
 * CREATE TABLE FX_SUB_ACCOUNT (
 *     ACCOUNT_NO  VARCHAR(14) NOT NULL REFERENCES FX_ACCOUNT,
 *     CURRENCY    CHAR(3)     NOT NULL,
 *     BALANCE     DECIMAL(18,2) NOT NULL,
 *     PRIMARY KEY (ACCOUNT_NO, CURRENCY)
 * );
 */
public class FxSubAccountRow {

    public String accountNo;
    public String currency;
    public BigDecimal balance;

    public FxSubAccountRow(String accountNo, String currency, BigDecimal balance) {
        this.accountNo = accountNo;
        this.currency = currency;
        this.balance = balance;
    }
}
