package com.bank.domainmodel.account;

import com.bank.domainmodel.customer.CustomerId;
import com.bank.domainmodel.shared.CurrencyMismatchException;
import com.bank.domainmodel.shared.Money;

import java.util.Collections;
import java.util.Currency;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 外幣綜合帳戶聚合根：一個帳戶下有多個幣別子帳（台灣銀行業常見設計）。
 *
 * Data Model 途徑得為每個幣別各開一列 ACCOUNT 資料；
 * 這裡「一戶多幣」是模型的第一級概念，
 * 「入錯幣別自動歸入正確子帳、未開子帳先開立」這些規則都收在聚合內。
 */
public class ForeignCurrencyAccount {

    public enum Status { ACTIVE, FROZEN, CLOSED }

    private final AccountNumber accountNumber;
    private final CustomerId ownerId;
    private final Map<Currency, Money> subAccounts = new LinkedHashMap<>();
    private Status status;

    public ForeignCurrencyAccount(AccountNumber accountNumber, CustomerId ownerId) {
        this.accountNumber = Objects.requireNonNull(accountNumber);
        this.ownerId = Objects.requireNonNull(ownerId);
        this.status = Status.ACTIVE;
    }

    /** 供 Repository 從儲存資料（主檔＋子帳明細）重建聚合。 */
    public static ForeignCurrencyAccount restore(AccountNumber accountNumber, CustomerId ownerId,
                                                 Status status, Map<Currency, Money> balances) {
        ForeignCurrencyAccount account = new ForeignCurrencyAccount(accountNumber, ownerId);
        account.status = Objects.requireNonNull(status);
        account.subAccounts.putAll(balances);
        return account;
    }

    /** 外幣帳戶不收台幣 —— 這條規則只寫這一次，寫在它該在的地方。 */
    public void deposit(Money money) {
        assertActive();
        if (money.currency().equals(Money.TWD)) {
            throw new CurrencyMismatchException("外幣帳戶不可存入台幣");
        }
        if (!money.isPositive()) {
            throw new IllegalArgumentException("存款金額必須為正數");
        }
        subAccounts.merge(money.currency(), money, Money::add);
    }

    public void withdraw(Money money) {
        assertActive();
        Money balance = balanceOf(money.currency());
        if (balance.isLessThan(money)) {
            throw new InsufficientBalanceException(
                    money.currency() + " 子帳餘額不足：餘額 " + balance + "，提領 " + money);
        }
        subAccounts.put(money.currency(), balance.subtract(money));
    }

    public Money balanceOf(Currency currency) {
        return subAccounts.getOrDefault(currency, Money.zero(currency));
    }

    public Map<Currency, Money> allBalances() {
        return Collections.unmodifiableMap(subAccounts);
    }

    public void freeze() {
        this.status = Status.FROZEN;
    }

    private void assertActive() {
        if (status != Status.ACTIVE) {
            throw new IllegalStateException("帳戶狀態 " + status + " 不允許交易");
        }
    }

    public AccountNumber accountNumber() { return accountNumber; }

    public CustomerId ownerId() { return ownerId; }

    public Status status() { return status; }
}
