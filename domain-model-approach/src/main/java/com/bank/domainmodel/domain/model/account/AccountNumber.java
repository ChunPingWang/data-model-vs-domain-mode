package com.bank.domainmodel.domain.model.account;

import java.util.Objects;

public final class AccountNumber {

    private final String value;

    public AccountNumber(String value) {
        if (value == null || !value.matches("\\d{10,14}")) {
            throw new IllegalArgumentException("帳號格式錯誤: " + value);
        }
        this.value = value;
    }

    public String value() { return value; }

    @Override
    public boolean equals(Object o) {
        return o instanceof AccountNumber && value.equals(((AccountNumber) o).value);
    }

    @Override
    public int hashCode() { return Objects.hash(value); }

    @Override
    public String toString() { return value; }
}
