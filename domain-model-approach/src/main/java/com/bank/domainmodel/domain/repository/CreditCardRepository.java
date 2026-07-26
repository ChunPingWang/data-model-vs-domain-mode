package com.bank.domainmodel.domain.repository;

import com.bank.domainmodel.domain.model.card.CardNumber;
import com.bank.domainmodel.domain.model.card.CreditCard;

import java.util.Optional;

public interface CreditCardRepository {

    Optional<CreditCard> findByCardNumber(CardNumber cardNumber);

    void save(CreditCard card);
}
