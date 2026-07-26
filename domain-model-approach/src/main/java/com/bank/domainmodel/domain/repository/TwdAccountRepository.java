package com.bank.domainmodel.domain.repository;

import com.bank.domainmodel.domain.model.account.AccountNumber;
import com.bank.domainmodel.domain.model.account.TwdAccount;

import java.util.Optional;

/**
 * Repository 介面屬於領域層，實作（JPA/MyBatis/JDBC）放在基礎設施層。
 * 領域模型不知道也不關心資料存在哪、拆成幾張表 —— 依賴方向是反過來的。
 */
public interface TwdAccountRepository {

    Optional<TwdAccount> findByAccountNumber(AccountNumber accountNumber);

    void save(TwdAccount account);
}
