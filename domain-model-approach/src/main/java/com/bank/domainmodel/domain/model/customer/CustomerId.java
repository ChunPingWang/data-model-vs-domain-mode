package com.bank.domainmodel.domain.model.customer;

import java.util.Objects;

/**
 * 型別化的識別碼：讓「把卡號當客戶編號傳進去」在編譯期就報錯，
 * 而不是等到查無資料才發現。
 */
public final class CustomerId {

    private final String value;

    public CustomerId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("客戶編號不可為空");
        }
        this.value = value;
    }

    public String value() { return value; }

    @Override
    public boolean equals(Object o) {
        return o instanceof CustomerId && value.equals(((CustomerId) o).value);
    }

    @Override
    public int hashCode() { return Objects.hash(value); }

    @Override
    public String toString() { return value; }
}
