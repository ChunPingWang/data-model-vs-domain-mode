package com.bank.datamodel.service;

import com.bank.datamodel.entity.AccountDO;

import java.math.BigDecimal;

/**
 * 存款帳戶業務介面（Business Layer 的抽象）。
 *
 * 三層式的 SOLID 作法：Controller 依賴這個介面而非實作（DIP），
 * 帳戶、換匯、信用卡各一個介面（SRP/ISP），沒有萬用的上帝 Service。
 * 注意：即使遵守 SOLID，回傳值仍是資料表鏡射的 AccountDO——
 * 「資料形狀貫穿三層」是 Data Model 範式的本質，不是 SOLID 能解的。
 */
public interface AccountService {

    AccountDO getAccount(String accountNo);

    void deposit(String accountNo, String currency, BigDecimal amount);

    void withdraw(String accountNo, BigDecimal amount);
}
