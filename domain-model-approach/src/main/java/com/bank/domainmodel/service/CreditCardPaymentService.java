package com.bank.domainmodel.service;

import com.bank.domainmodel.account.TwdAccount;
import com.bank.domainmodel.card.CreditCard;
import com.bank.domainmodel.shared.Money;

/**
 * 信用卡繳款 Domain Service：協調「台幣帳戶扣款」與「信用卡銷帳」
 * 兩個聚合，本身不含任何一條屬於單一聚合的規則。
 */
public class CreditCardPaymentService {

    public void payBillFromAccount(TwdAccount account, CreditCard card, Money amount) {
        account.withdraw(amount);
        card.applyPayment(amount);
    }
}
