package com.bank.domainmodel.card;

import com.bank.domainmodel.customer.CustomerId;
import com.bank.domainmodel.shared.CurrencyMismatchException;
import com.bank.domainmodel.shared.Money;

import java.time.YearMonth;
import java.util.Objects;

/**
 * 信用卡聚合根。
 *
 * 「可用額度」是行為（availableCredit()），不是資料表欄位 ——
 * 定義只寫一次，全系統共用；Data Model 途徑則是每個模組各自
 * creditLimit - usedAmt 算一遍，改定義時要全域搜尋。
 */
public class CreditCard {

    public enum Status { ACTIVE, REPORTED_LOST, SUSPENDED }

    private final CardNumber cardNumber;
    private final CustomerId holderId;
    private final Money creditLimit;
    private final YearMonth expiry;
    private Money usedCredit;
    private Money currentBill;
    private Status status;

    public CreditCard(CardNumber cardNumber, CustomerId holderId,
                      Money creditLimit, YearMonth expiry) {
        if (!creditLimit.currency().equals(Money.TWD)) {
            throw new CurrencyMismatchException("信用額度以台幣計價");
        }
        this.cardNumber = Objects.requireNonNull(cardNumber);
        this.holderId = Objects.requireNonNull(holderId);
        this.creditLimit = creditLimit;
        this.expiry = Objects.requireNonNull(expiry);
        this.usedCredit = Money.zero(Money.TWD);
        this.currentBill = Money.zero(Money.TWD);
        this.status = Status.ACTIVE;
    }

    /** 刷卡授權：狀態、效期、額度三項檢查集中在唯一入口。 */
    public void authorize(Money amount, YearMonth today) {
        if (status != Status.ACTIVE) {
            throw new IllegalStateException("卡片狀態 " + status + " 不可交易");
        }
        if (today.isAfter(expiry)) {
            throw new IllegalStateException("卡片已過期");
        }
        if (availableCredit().isLessThan(amount)) {
            throw new CreditLimitExceededException(
                    "超過可用額度：可用 " + availableCredit() + "，請求 " + amount);
        }
        this.usedCredit = usedCredit.add(amount);
    }

    /** 繳款：只還信用卡自己的帳；扣哪個存款帳戶是應用層編排的事。 */
    public void applyPayment(Money amount) {
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("繳款金額必須為正數");
        }
        this.currentBill = currentBill.subtract(amount);
        this.usedCredit = usedCredit.subtract(amount);
        if (usedCredit.isNegative()) {
            this.usedCredit = Money.zero(Money.TWD);
        }
    }

    /** 出帳：把已用額度結轉為本期帳單。 */
    public void issueBill() {
        this.currentBill = usedCredit;
    }

    public void reportLost() {
        this.status = Status.REPORTED_LOST;
    }

    public Money availableCredit() {
        return creditLimit.subtract(usedCredit);
    }

    public CardNumber cardNumber() { return cardNumber; }

    public CustomerId holderId() { return holderId; }

    public Money creditLimit() { return creditLimit; }

    public Money currentBill() { return currentBill; }

    public Status status() { return status; }
}
