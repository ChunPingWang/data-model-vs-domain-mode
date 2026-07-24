package com.bank.datamodel.entity;

import java.time.LocalDateTime;

/**
 * CUSTOMER 資料表的一對一鏡射（Data Object）。
 * 只有欄位與 getter/setter，沒有任何業務行為 —— 典型的「貧血模型」。
 */
public class CustomerDO {

    private String customerId;
    private String idNo;
    private String name;
    private String phone;
    private String email;
    private String vipFlag;   // "Y" / "N"
    private String status;    // "A" / "S" / "C"
    private LocalDateTime createdAt;

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getIdNo() { return idNo; }
    public void setIdNo(String idNo) { this.idNo = idNo; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getVipFlag() { return vipFlag; }
    public void setVipFlag(String vipFlag) { this.vipFlag = vipFlag; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
