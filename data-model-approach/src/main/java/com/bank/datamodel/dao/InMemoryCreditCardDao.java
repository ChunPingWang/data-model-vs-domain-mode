package com.bank.datamodel.dao;

import com.bank.datamodel.entity.CreditCardDO;

import java.util.HashMap;
import java.util.Map;

public class InMemoryCreditCardDao implements CreditCardDao {

    private final Map<String, CreditCardDO> creditCardTable = new HashMap<>();

    @Override
    public CreditCardDO findByCardNo(String cardNo) {
        return creditCardTable.get(cardNo);
    }

    @Override
    public void update(CreditCardDO card) {
        creditCardTable.put(card.getCardNo(), card);
    }

    @Override
    public void insert(CreditCardDO card) {
        creditCardTable.put(card.getCardNo(), card);
    }
}
