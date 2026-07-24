package com.bank.datamodel.service;

import com.bank.datamodel.dao.AccountDao;
import com.bank.datamodel.dao.CreditCardDao;
import com.bank.datamodel.entity.AccountDO;
import com.bank.datamodel.entity.CreditCardDO;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Data Model 途徑的典型寫法：Transaction Script。
 *
 * 所有業務規則都集中在 Service 裡，用 if/else 判斷字串型的
 * acctType / status / currency 欄位碼；Entity 只是資料的載體。
 *
 * 問題會隨業務成長浮現：
 *  1. 「餘額不可為負」「停卡不能刷」等不變量散落在各方法，漏寫一處就出資料洞。
 *  2. 台幣、外幣、信用卡三種業務全部糾纏在同一個類別。
 *  3. 金額是裸的 BigDecimal，幣別是另一個 String 欄位，
 *     「美元金額加到台幣餘額」這種錯誤編譯器完全擋不住。
 */
public class BankingService {

    private static final String TYPE_TWD_DEMAND  = "01";
    private static final String TYPE_TWD_FIXED   = "02";
    private static final String TYPE_FX_DEMAND   = "03";

    private final AccountDao accountDao;
    private final CreditCardDao creditCardDao;

    public BankingService(AccountDao accountDao, CreditCardDao creditCardDao) {
        this.accountDao = accountDao;
        this.creditCardDao = creditCardDao;
    }

    /** 存款：台幣、外幣共用一支，靠欄位碼分流。 */
    public void deposit(String accountNo, String currency, BigDecimal amount) {
        AccountDO acct = accountDao.findByAccountNo(accountNo);
        if (acct == null) {
            throw new IllegalArgumentException("帳戶不存在: " + accountNo);
        }
        if (!"A".equals(acct.getStatus())) {
            throw new IllegalStateException("帳戶狀態不允許交易: " + acct.getStatus());
        }
        if (TYPE_TWD_DEMAND.equals(acct.getAcctType()) || TYPE_TWD_FIXED.equals(acct.getAcctType())) {
            if (!"TWD".equals(currency)) {
                throw new IllegalArgumentException("台幣帳戶只能存入台幣");
            }
        } else if (TYPE_FX_DEMAND.equals(acct.getAcctType())) {
            if (!acct.getCurrency().equals(currency)) {
                throw new IllegalArgumentException("幣別不符，此外幣帳戶幣別為 " + acct.getCurrency());
            }
        } else {
            throw new IllegalStateException("未知帳戶類型: " + acct.getAcctType());
        }
        acct.setBalance(acct.getBalance().add(amount));
        accountDao.update(acct);
        // TODO 寫 TRANSACTION_LOG —— log 的一致性也要 Service 自己記得
    }

    /** 提款：同樣一長串防衛式檢查，且和 deposit 的檢查有大量重複。 */
    public void withdraw(String accountNo, BigDecimal amount) {
        AccountDO acct = accountDao.findByAccountNo(accountNo);
        if (acct == null) {
            throw new IllegalArgumentException("帳戶不存在: " + accountNo);
        }
        if (!"A".equals(acct.getStatus())) {
            throw new IllegalStateException("帳戶狀態不允許交易: " + acct.getStatus());
        }
        if (TYPE_TWD_FIXED.equals(acct.getAcctType())) {
            throw new IllegalStateException("定存帳戶不可直接提領，須先解約");
        }
        if (acct.getBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("餘額不足");
        }
        acct.setBalance(acct.getBalance().subtract(amount));
        accountDao.update(acct);
    }

    /** 台幣結購外幣：兩個帳戶的扣款/入帳與匯率計算全部攤平在同一支流程。 */
    public void buyForeignCurrency(String twdAccountNo, String fxAccountNo,
                                   BigDecimal twdAmount, BigDecimal exchangeRate) {
        AccountDO twd = accountDao.findByAccountNo(twdAccountNo);
        AccountDO fx  = accountDao.findByAccountNo(fxAccountNo);

        if (twd == null || fx == null) {
            throw new IllegalArgumentException("帳戶不存在");
        }
        if (!TYPE_TWD_DEMAND.equals(twd.getAcctType())) {
            throw new IllegalArgumentException("扣款帳戶必須是台幣活存");
        }
        if (!TYPE_FX_DEMAND.equals(fx.getAcctType())) {
            throw new IllegalArgumentException("入帳帳戶必須是外幣帳戶");
        }
        if (!"A".equals(twd.getStatus()) || !"A".equals(fx.getStatus())) {
            throw new IllegalStateException("帳戶狀態不允許交易");
        }
        if (twd.getBalance().compareTo(twdAmount) < 0) {
            throw new IllegalStateException("台幣餘額不足");
        }

        BigDecimal fxAmount = twdAmount.divide(exchangeRate, 2, RoundingMode.DOWN);

        twd.setBalance(twd.getBalance().subtract(twdAmount));
        fx.setBalance(fx.getBalance().add(fxAmount));
        accountDao.update(twd);
        accountDao.update(fx);
    }

    /** 信用卡刷卡授權。 */
    public void chargeCreditCard(String cardNo, BigDecimal amount) {
        CreditCardDO card = creditCardDao.findByCardNo(cardNo);
        if (card == null) {
            throw new IllegalArgumentException("卡片不存在: " + cardNo);
        }
        if (!"A".equals(card.getStatus())) {
            throw new IllegalStateException("卡片已掛失或停用");
        }
        // 「可用額度」在這裡臨時算一次；別的模組（分期、預借現金）還會各算各的
        BigDecimal available = card.getCreditLimit().subtract(card.getUsedAmt());
        if (available.compareTo(amount) < 0) {
            throw new IllegalStateException("超過可用額度");
        }
        card.setUsedAmt(card.getUsedAmt().add(amount));
        creditCardDao.update(card);
    }

    /** 從台幣帳戶扣款繳信用卡帳單：跨兩張表的更新，一致性全靠這支方法自律。 */
    public void payCreditCardBill(String cardNo, String twdAccountNo, BigDecimal amount) {
        CreditCardDO card = creditCardDao.findByCardNo(cardNo);
        AccountDO acct = accountDao.findByAccountNo(twdAccountNo);

        if (card == null || acct == null) {
            throw new IllegalArgumentException("卡片或帳戶不存在");
        }
        if (!"TWD".equals(acct.getCurrency())) {
            throw new IllegalArgumentException("繳款帳戶必須是台幣帳戶");
        }
        if (acct.getBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("餘額不足");
        }

        acct.setBalance(acct.getBalance().subtract(amount));
        card.setBillAmt(card.getBillAmt().subtract(amount));
        card.setUsedAmt(card.getUsedAmt().subtract(amount));

        accountDao.update(acct);
        creditCardDao.update(card);
    }
}
