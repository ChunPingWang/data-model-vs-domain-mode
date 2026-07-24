package com.bank.domainmodel.customer;

import java.util.Objects;

/** 聯絡資訊 Value Object：不可變，換值就換整個物件。 */
public final class ContactInfo {

    private final String phone;
    private final String email;

    public ContactInfo(String phone, String email) {
        this.phone = Objects.requireNonNull(phone);
        this.email = Objects.requireNonNull(email);
    }

    public String phone() { return phone; }

    public String email() { return email; }
}
