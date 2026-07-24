package com.bank.domainmodel.repository;

import com.bank.domainmodel.account.AccountNumber;
import com.bank.domainmodel.account.ForeignCurrencyAccount;

import java.util.Optional;

public interface ForeignCurrencyAccountRepository {

    Optional<ForeignCurrencyAccount> findByAccountNumber(AccountNumber accountNumber);

    void save(ForeignCurrencyAccount account);
}
