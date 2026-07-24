package com.bank.domainmodel.customer;

import java.util.Objects;

/**
 * 客戶聚合根。
 *
 * 注意：Customer 不直接持有帳戶與卡片的物件集合，
 * 只被其他聚合以 CustomerId 參照 —— 聚合之間以 ID 相連，
 * 才能維持各自獨立的交易邊界（一次交易只鎖一個聚合）。
 */
public class Customer {

    public enum Status { ACTIVE, SUSPENDED, CLOSED }

    private final CustomerId id;
    private final String idNo;
    private String name;
    private ContactInfo contactInfo;
    private boolean vip;
    private Status status;

    public Customer(CustomerId id, String idNo, String name, ContactInfo contactInfo) {
        this.id = Objects.requireNonNull(id);
        this.idNo = Objects.requireNonNull(idNo);
        this.name = Objects.requireNonNull(name);
        this.contactInfo = Objects.requireNonNull(contactInfo);
        this.vip = false;
        this.status = Status.ACTIVE;
    }

    /** 業務行為有名字：「升等為 VIP」而不是 setVipFlag("Y")。 */
    public void upgradeToVip() {
        if (status != Status.ACTIVE) {
            throw new IllegalStateException("非正常戶不可升等 VIP");
        }
        this.vip = true;
    }

    public void suspend() {
        if (status == Status.CLOSED) {
            throw new IllegalStateException("已結清客戶不可再變更狀態");
        }
        this.status = Status.SUSPENDED;
    }

    public void updateContactInfo(ContactInfo newInfo) {
        this.contactInfo = Objects.requireNonNull(newInfo);
    }

    public boolean canOpenNewAccount() {
        return status == Status.ACTIVE;
    }

    public CustomerId id() { return id; }

    public String idNo() { return idNo; }

    public String name() { return name; }

    public ContactInfo contactInfo() { return contactInfo; }

    public boolean isVip() { return vip; }

    public Status status() { return status; }
}
