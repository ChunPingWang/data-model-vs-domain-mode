package com.bank.datamodel.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * CREDIT_CARD 資料表的鏡射。
 * 「可用額度 = 信用額度 - 已用額度」這個衍生概念不存在，
 * 每個用到的地方都要自己重算一次。
 */
public class CreditCardDO {

    private String cardNo;
    private String customerId;
    private String cardType;      // "01" 普卡 / "02" 金卡 / "03" 白金卡
    private BigDecimal creditLimit;
    private BigDecimal usedAmt;
    private BigDecimal billAmt;
    private String status;        // "A" / "B" / "S"
    private LocalDate expireDate;

    public String getCardNo() { return cardNo; }
    public void setCardNo(String cardNo) { this.cardNo = cardNo; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getCardType() { return cardType; }
    public void setCardType(String cardType) { this.cardType = cardType; }

    public BigDecimal getCreditLimit() { return creditLimit; }
    public void setCreditLimit(BigDecimal creditLimit) { this.creditLimit = creditLimit; }

    public BigDecimal getUsedAmt() { return usedAmt; }
    public void setUsedAmt(BigDecimal usedAmt) { this.usedAmt = usedAmt; }

    public BigDecimal getBillAmt() { return billAmt; }
    public void setBillAmt(BigDecimal billAmt) { this.billAmt = billAmt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDate getExpireDate() { return expireDate; }
    public void setExpireDate(LocalDate expireDate) { this.expireDate = expireDate; }
}
