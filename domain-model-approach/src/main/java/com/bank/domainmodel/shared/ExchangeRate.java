package com.bank.domainmodel.shared;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * 匯率 Value Object：明確表達「1 單位 quote 幣 = rate 單位 base 幣」，
 * 並把換算方向封裝起來，避免呼叫端乘除方向弄反。
 */
public final class ExchangeRate {

    private final Currency base;    // 例：TWD
    private final Currency quote;   // 例：USD
    private final BigDecimal rate;  // 例：32.5 （1 USD = 32.5 TWD）

    public ExchangeRate(Currency base, Currency quote, BigDecimal rate) {
        if (rate.signum() <= 0) {
            throw new IllegalArgumentException("匯率必須為正數");
        }
        this.base = Objects.requireNonNull(base);
        this.quote = Objects.requireNonNull(quote);
        this.rate = rate;
    }

    /** 以 base 幣金額換得 quote 幣金額（例：台幣 → 美元）。 */
    public Money convert(Money baseMoney) {
        if (!baseMoney.currency().equals(base)) {
            throw new CurrencyMismatchException(
                    "此匯率的扣款幣別為 " + base + "，不是 " + baseMoney.currency());
        }
        BigDecimal converted = baseMoney.amount().divide(rate, 2, RoundingMode.DOWN);
        return Money.of(converted, quote);
    }

    public Currency base() { return base; }

    public Currency quote() { return quote; }

    public BigDecimal rate() { return rate; }
}
