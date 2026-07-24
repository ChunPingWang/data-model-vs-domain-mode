package com.bank.domainmodel.service;

import com.bank.domainmodel.account.ForeignCurrencyAccount;
import com.bank.domainmodel.account.TwdAccount;
import com.bank.domainmodel.shared.ExchangeRate;
import com.bank.domainmodel.shared.Money;

/**
 * 換匯 Domain Service。
 *
 * 「台幣結購外幣」橫跨兩個聚合，不適合放進任何一邊，
 * 所以放在 Domain Service —— 但它只負責「編排」：
 * 扣款規則歸台幣帳戶管、入帳規則歸外幣帳戶管、匯率換算歸 ExchangeRate 管。
 * 對照 Data Model 途徑：同一件事是一支 100 行的 Transaction Script。
 */
public class CurrencyExchangeService {

    /** 以台幣結購外幣，回傳實際購得的外幣金額。 */
    public Money buyForeignCurrency(TwdAccount from, ForeignCurrencyAccount to,
                                    Money twdAmount, ExchangeRate rate) {
        Money fxAmount = rate.convert(twdAmount);

        from.withdraw(twdAmount);   // 餘額不足、非台幣、帳戶凍結 → 聚合自己擋
        to.deposit(fxAmount);       // 存入台幣、帳戶凍結 → 聚合自己擋

        return fxAmount;
    }
}
