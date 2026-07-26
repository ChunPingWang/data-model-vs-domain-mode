package com.bank.datamodel.service.impl;

import com.bank.datamodel.dao.AccountDao;
import com.bank.datamodel.entity.AccountDO;
import com.bank.datamodel.service.AccountService;

import java.math.BigDecimal;

/**
 * 存提款 Transaction Script。
 *
 * SOLID 之後仍是資料導向：規則以 if/else 檢查欄位碼的形式活在 Service，
 * deposit 與 withdraw 各自重複「狀態檢查」，且擋不住別人繞過 Service 直接 set 欄位。
 */
public class AccountServiceImpl implements AccountService {

    private static final String TYPE_TWD_DEMAND = "01";
    private static final String TYPE_TWD_FIXED  = "02";
    private static final String TYPE_FX_DEMAND  = "03";

    private final AccountDao accountDao;

    public AccountServiceImpl(AccountDao accountDao) {
        this.accountDao = accountDao;
    }

    @Override
    public AccountDO getAccount(String accountNo) {
        return accountDao.findByAccountNo(accountNo);
    }

    @Override
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
    }

    @Override
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
}
