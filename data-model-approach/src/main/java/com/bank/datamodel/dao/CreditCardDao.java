package com.bank.datamodel.dao;

import com.bank.datamodel.entity.CreditCardDO;

public interface CreditCardDao {

    CreditCardDO findByCardNo(String cardNo);

    void update(CreditCardDO card);
}
