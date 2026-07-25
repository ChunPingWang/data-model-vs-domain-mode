package com.bank.datamodel.web;

import com.bank.datamodel.dao.AccountDao;
import com.bank.datamodel.entity.AccountDO;
import com.bank.datamodel.service.BankingService;

import java.math.BigDecimal;

/**
 * Data Model 途徑的前端進入點（示意；實務上是 @RestController）。
 *
 * 典型寫法：查詢直接把 DO 序列化回前端 ——
 * 資料表的形狀一路穿透到畫面，ACCT_TYPE="03"、STATUS="A" 這些碼值
 * 由前端自行對照碼表翻譯成「外幣活存」「正常」。
 * 副作用：API 契約 = 資料表 schema，改表就是改 API。
 */
public class AccountController {

    private final AccountDao accountDao;
    private final BankingService bankingService;

    public AccountController(AccountDao accountDao, BankingService bankingService) {
        this.accountDao = accountDao;
        this.bankingService = bankingService;
    }

    /** GET /accounts/{accountNo} —— 回傳值就是資料表的一列。 */
    public AccountDO getAccount(String accountNo) {
        return accountDao.findByAccountNo(accountNo);
    }

    /**
     * POST /fx/purchase —— 參數全是字串與裸數字，
     * 帳號傳反、金額幣別對不上，要到執行期（甚至對帳時）才會發現。
     */
    public void buyForeignCurrency(String twdAccountNo, String fxAccountNo,
                                   BigDecimal twdAmount, BigDecimal exchangeRate) {
        bankingService.buyForeignCurrency(twdAccountNo, fxAccountNo, twdAmount, exchangeRate);
    }
}
