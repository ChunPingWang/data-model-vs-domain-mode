package com.bank.datamodel.web;

import com.bank.datamodel.entity.AccountDO;
import com.bank.datamodel.service.AccountService;
import com.bank.datamodel.service.FxService;

import java.math.BigDecimal;

/**
 * 三層式的 Presentation Layer（示意；實務上是 @RestController）。
 *
 * SOLID 版本：只依賴 Service「介面」（DIP），不再直接碰 DAO。
 * 但 Data Model 範式的本質仍在——getAccount() 回傳的是資料表鏡射的
 * AccountDO，ACCT_TYPE="03"、STATUS="A" 這些碼值由前端自行翻譯，
 * API 契約仍然等於資料表 schema。
 */
public class AccountController {

    private final AccountService accountService;   // 介面，不是實作
    private final FxService fxService;             // 介面，不是實作

    public AccountController(AccountService accountService, FxService fxService) {
        this.accountService = accountService;
        this.fxService = fxService;
    }

    /** GET /accounts/{accountNo} —— 回傳值就是資料表的一列。 */
    public AccountDO getAccount(String accountNo) {
        return accountService.getAccount(accountNo);
    }

    /** POST /fx/purchase —— 參數全是字串與裸數字，帳號傳反要到執行期才發現。 */
    public void buyForeignCurrency(String twdAccountNo, String fxAccountNo,
                                   BigDecimal twdAmount, BigDecimal exchangeRate) {
        fxService.buyForeignCurrency(twdAccountNo, fxAccountNo, twdAmount, exchangeRate);
    }
}
