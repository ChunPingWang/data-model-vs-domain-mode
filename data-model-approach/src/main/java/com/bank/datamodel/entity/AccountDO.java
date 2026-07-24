package com.bank.datamodel.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * ACCOUNT 資料表的鏡射。
 * 台幣帳戶與外幣帳戶共用同一個類別，靠 acctType / currency 欄位區分；
 * 「外幣帳戶不能存台幣」這類規則這裡完全表達不出來。
 */
public class AccountDO {

    private String accountNo;
    private String customerId;
    private String acctType;   // "01" 台幣活存 / "02" 台幣定存 / "03" 外幣活存
    private String currency;   // "TWD" / "USD" / "JPY" ...
    private BigDecimal balance;
    private BigDecimal interestRate;
    private String status;     // "A" / "F" / "C"
    private LocalDate openDate;

    public String getAccountNo() { return accountNo; }
    public void setAccountNo(String accountNo) { this.accountNo = accountNo; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getAcctType() { return acctType; }
    public void setAcctType(String acctType) { this.acctType = acctType; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    public BigDecimal getInterestRate() { return interestRate; }
    public void setInterestRate(BigDecimal interestRate) { this.interestRate = interestRate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDate getOpenDate() { return openDate; }
    public void setOpenDate(LocalDate openDate) { this.openDate = openDate; }
}
