package com.bank.domainmodel.domain.model.card;

import java.util.Objects;

public final class CardNumber {

    private final String value;

    public CardNumber(String value) {
        if (value == null || !value.matches("\\d{16}")) {
            throw new IllegalArgumentException("卡號必須為 16 位數字");
        }
        this.value = value;
    }

    public String value() { return value; }

    /** 對外顯示一律遮罩，避免卡號誤入 log。 */
    public String masked() {
        return value.substring(0, 6) + "******" + value.substring(12);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof CardNumber && value.equals(((CardNumber) o).value);
    }

    @Override
    public int hashCode() { return Objects.hash(value); }

    @Override
    public String toString() { return masked(); }
}
