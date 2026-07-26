package com.bank.domainmodel.adapter.out.persistence;

import com.bank.domainmodel.domain.model.account.AccountNumber;
import com.bank.domainmodel.domain.model.account.TwdAccount;
import com.bank.domainmodel.domain.model.customer.CustomerId;
import com.bank.domainmodel.domain.repository.TwdAccountRepository;
import com.bank.domainmodel.domain.model.shared.Money;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 台幣帳戶聚合剛好一張表就存得下（TWD_ACCOUNT），
 * 但那是實作上的巧合，不是規則——Repository 的契約單位始終是聚合。
 */
public class InMemoryTwdAccountRepository implements TwdAccountRepository {

    private static class TwdAccountRow {
        String accountNo;
        String customerId;
        BigDecimal balance;
        String status;

        TwdAccountRow(String accountNo, String customerId, BigDecimal balance, String status) {
            this.accountNo = accountNo;
            this.customerId = customerId;
            this.balance = balance;
            this.status = status;
        }
    }

    private final Map<String, TwdAccountRow> twdAccountTable = new HashMap<>();

    @Override
    public void save(TwdAccount aggregate) {
        twdAccountTable.put(aggregate.accountNumber().value(), new TwdAccountRow(
                aggregate.accountNumber().value(),
                aggregate.ownerId().value(),
                aggregate.balance().amount(),
                aggregate.status().name()));
    }

    @Override
    public Optional<TwdAccount> findByAccountNumber(AccountNumber accountNumber) {
        TwdAccountRow row = twdAccountTable.get(accountNumber.value());
        if (row == null) {
            return Optional.empty();
        }
        return Optional.of(TwdAccount.restore(
                new AccountNumber(row.accountNo),
                new CustomerId(row.customerId),
                Money.of(row.balance, Money.TWD),
                TwdAccount.Status.valueOf(row.status)));
    }
}
