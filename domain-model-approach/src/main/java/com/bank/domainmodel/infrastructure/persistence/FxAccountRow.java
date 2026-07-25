package com.bank.domainmodel.infrastructure.persistence;

/**
 * FX_ACCOUNT 主檔資料表的一列（基礎設施層的私有細節，領域層看不到）。
 *
 * CREATE TABLE FX_ACCOUNT (
 *     ACCOUNT_NO  VARCHAR(14) PRIMARY KEY,
 *     CUSTOMER_ID VARCHAR(10) NOT NULL,
 *     STATUS      VARCHAR(10) NOT NULL
 * );
 */
public class FxAccountRow {

    public String accountNo;
    public String customerId;
    public String status;

    public FxAccountRow(String accountNo, String customerId, String status) {
        this.accountNo = accountNo;
        this.customerId = customerId;
        this.status = status;
    }
}
