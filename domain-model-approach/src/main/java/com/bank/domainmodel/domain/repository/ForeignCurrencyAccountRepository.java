package com.bank.domainmodel.domain.repository;

import com.bank.domainmodel.domain.model.account.AccountNumber;
import com.bank.domainmodel.domain.model.account.ForeignCurrencyAccount;

import java.util.Optional;

public interface ForeignCurrencyAccountRepository {

    Optional<ForeignCurrencyAccount> findByAccountNumber(AccountNumber accountNumber);

    void save(ForeignCurrencyAccount account);
}
