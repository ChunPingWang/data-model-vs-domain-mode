package com.bank.domainmodel.repository;

import com.bank.domainmodel.card.CardNumber;
import com.bank.domainmodel.card.CreditCard;

import java.util.Optional;

public interface CreditCardRepository {

    Optional<CreditCard> findByCardNumber(CardNumber cardNumber);

    void save(CreditCard card);
}
