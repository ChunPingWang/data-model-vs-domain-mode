package com.bank.datamodel.service.impl;

import com.bank.datamodel.dao.AccountDao;
import com.bank.datamodel.dao.CreditCardDao;
import com.bank.datamodel.entity.AccountDO;
import com.bank.datamodel.entity.CreditCardDO;
import com.bank.datamodel.service.CreditCardService;

import java.math.BigDecimal;

/** 信用卡 Transaction Script。 */
public class CreditCardServiceImpl implements CreditCardService {

    private final CreditCardDao creditCardDao;
    private final AccountDao accountDao;

    public CreditCardServiceImpl(CreditCardDao creditCardDao, AccountDao accountDao) {
        this.creditCardDao = creditCardDao;
        this.accountDao = accountDao;
    }

    @Override
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

    @Override
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
