package com.bank.datamodel.service.impl;

import com.bank.datamodel.dao.AccountDao;
import com.bank.datamodel.entity.AccountDO;
import com.bank.datamodel.service.FxService;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** 換匯 Transaction Script：兩個帳戶的扣款/入帳與匯率計算攤平在同一支流程。 */
public class FxServiceImpl implements FxService {

    private static final String TYPE_TWD_DEMAND = "01";
    private static final String TYPE_FX_DEMAND  = "03";

    private final AccountDao accountDao;

    public FxServiceImpl(AccountDao accountDao) {
        this.accountDao = accountDao;
    }

    @Override
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
}
