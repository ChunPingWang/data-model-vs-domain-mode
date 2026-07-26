package com.bank.domainmodel.domain.model.shared;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * 金額 Value Object：金額與幣別永遠綁在一起。
 *
 * Data Model 途徑用 BigDecimal + String currency 兩個獨立欄位，
 * 「USD 100 加進 TWD 餘額」要靠工程師自律；
 * 這裡改由型別系統保證 —— 不同幣別相加直接丟例外，錯誤在測試期就爆開。
 */
public final class Money implements Comparable<Money> {

    public static final Currency TWD = Currency.getInstance("TWD");
    public static final Currency USD = Currency.getInstance("USD");

    private final BigDecimal amount;
    private final Currency currency;

    private Money(BigDecimal amount, Currency currency) {
        Objects.requireNonNull(amount, "金額不可為 null");
        Objects.requireNonNull(currency, "幣別不可為 null");
        this.amount = amount.setScale(currency.getDefaultFractionDigits() >= 0
                ? currency.getDefaultFractionDigits() : 2, RoundingMode.HALF_UP);
        this.currency = currency;
    }

    public static Money of(BigDecimal amount, Currency currency) {
        return new Money(amount, currency);
    }

    public static Money twd(String amount) {
        return new Money(new BigDecimal(amount), TWD);
    }

    public static Money zero(Currency currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    public Money add(Money other) {
        assertSameCurrency(other);
        return new Money(this.amount.add(other.amount), currency);
    }

    public Money subtract(Money other) {
        assertSameCurrency(other);
        return new Money(this.amount.subtract(other.amount), currency);
    }

    public boolean isLessThan(Money other) {
        assertSameCurrency(other);
        return this.amount.compareTo(other.amount) < 0;
    }

    public boolean isNegative() {
        return amount.signum() < 0;
    }

    public boolean isPositive() {
        return amount.signum() > 0;
    }

    public BigDecimal amount() { return amount; }

    public Currency currency() { return currency; }

    private void assertSameCurrency(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new CurrencyMismatchException(
                    "幣別不一致: " + this.currency + " vs " + other.currency);
        }
    }

    @Override
    public int compareTo(Money other) {
        assertSameCurrency(other);
        return this.amount.compareTo(other.amount);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Money)) return false;
        Money money = (Money) o;
        return amount.compareTo(money.amount) == 0 && currency.equals(money.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount.stripTrailingZeros(), currency);
    }

    @Override
    public String toString() {
        return currency.getCurrencyCode() + " " + amount;
    }
}
