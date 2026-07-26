package com.bank.domainmodel.domain.model.account;

import com.bank.domainmodel.domain.model.customer.CustomerId;
import com.bank.domainmodel.domain.model.shared.CurrencyMismatchException;
import com.bank.domainmodel.domain.model.shared.Money;

import java.util.Objects;

/**
 * 台幣活存帳戶聚合根。
 *
 * 與 Data Model 途徑最大的差別：
 *  1. 台幣帳戶是獨立的「型別」，不是 ACCOUNT 表裡的一個 acctType 碼。
 *  2. 「只收台幣」「餘額不可為負」「凍結不能動」等不變量由聚合自己守住，
 *     任何呼叫端都繞不過去 —— 不必指望每支 Service 都記得檢查。
 *  3. 餘額（balance）對外唯讀，狀態只能透過具業務語意的行為改變。
 */
public class TwdAccount {

    public enum Status { ACTIVE, FROZEN, CLOSED }

    private final AccountNumber accountNumber;
    private final CustomerId ownerId;
    private Money balance;
    private Status status;

    public TwdAccount(AccountNumber accountNumber, CustomerId ownerId) {
        this.accountNumber = Objects.requireNonNull(accountNumber);
        this.ownerId = Objects.requireNonNull(ownerId);
        this.balance = Money.zero(Money.TWD);
        this.status = Status.ACTIVE;
    }

    /** 供 Repository 從儲存資料重建聚合；只做重建，不觸發業務行為。 */
    public static TwdAccount restore(AccountNumber accountNumber, CustomerId ownerId,
                                     Money balance, Status status) {
        TwdAccount account = new TwdAccount(accountNumber, ownerId);
        account.balance = Objects.requireNonNull(balance);
        account.status = Objects.requireNonNull(status);
        return account;
    }

    public void deposit(Money money) {
        assertActive();
        if (!money.currency().equals(Money.TWD)) {
            throw new CurrencyMismatchException("台幣帳戶只接受台幣存款");
        }
        if (!money.isPositive()) {
            throw new IllegalArgumentException("存款金額必須為正數");
        }
        this.balance = balance.add(money);
    }

    public void withdraw(Money money) {
        assertActive();
        if (!money.isPositive()) {
            throw new IllegalArgumentException("提款金額必須為正數");
        }
        if (balance.isLessThan(money)) {
            throw new InsufficientBalanceException(
                    "餘額不足：餘額 " + balance + "，提領 " + money);
        }
        this.balance = balance.subtract(money);
    }

    public void freeze() {
        if (status == Status.CLOSED) {
            throw new IllegalStateException("已結清帳戶不可凍結");
        }
        this.status = Status.FROZEN;
    }

    public void close() {
        if (balance.isPositive()) {
            throw new IllegalStateException("餘額不為零，不可結清");
        }
        this.status = Status.CLOSED;
    }

    private void assertActive() {
        if (status != Status.ACTIVE) {
            throw new IllegalStateException("帳戶狀態 " + status + " 不允許交易");
        }
    }

    public AccountNumber accountNumber() { return accountNumber; }

    public CustomerId ownerId() { return ownerId; }

    public Money balance() { return balance; }

    public Status status() { return status; }
}
