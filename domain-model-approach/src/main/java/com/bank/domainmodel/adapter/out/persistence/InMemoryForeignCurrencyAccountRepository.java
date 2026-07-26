package com.bank.domainmodel.adapter.out.persistence;

import com.bank.domainmodel.domain.model.account.AccountNumber;
import com.bank.domainmodel.domain.model.account.ForeignCurrencyAccount;
import com.bank.domainmodel.domain.model.customer.CustomerId;
import com.bank.domainmodel.domain.repository.ForeignCurrencyAccountRepository;
import com.bank.domainmodel.domain.model.shared.Money;

import java.util.ArrayList;
import java.util.Currency;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Repository 實作：對照 Data Model 途徑「一個 DAO 對應一張表」，
 * 這裡是「一個 Repository 對應一個聚合」——
 *
 *   save(aggregate) 傳入的是【整個 ForeignCurrencyAccount 聚合】，
 *   內部拆解寫入兩張表：FX_ACCOUNT 主檔一列 + FX_SUB_ACCOUNT 每幣別一列；
 *   findByAccountNumber() 再從兩張表把聚合完整組回來。
 *
 * 呼叫端（Application Service）完全不知道也不需要知道背後是幾張表。
 * 此處以兩個 Map 模擬兩張資料表，換成 JDBC/JPA 只是替換這個類別。
 */
public class InMemoryForeignCurrencyAccountRepository implements ForeignCurrencyAccountRepository {

    private final Map<String, FxAccountRow> fxAccountTable = new HashMap<>();
    private final Map<String, List<FxSubAccountRow>> fxSubAccountTable = new HashMap<>();

    @Override
    public void save(ForeignCurrencyAccount aggregate) {
        String accountNo = aggregate.accountNumber().value();

        fxAccountTable.put(accountNo, new FxAccountRow(
                accountNo,
                aggregate.ownerId().value(),
                aggregate.status().name()));

        List<FxSubAccountRow> subRows = new ArrayList<>();
        aggregate.allBalances().forEach((currency, money) ->
                subRows.add(new FxSubAccountRow(
                        accountNo, currency.getCurrencyCode(), money.amount())));
        fxSubAccountTable.put(accountNo, subRows);
    }

    @Override
    public Optional<ForeignCurrencyAccount> findByAccountNumber(AccountNumber accountNumber) {
        FxAccountRow master = fxAccountTable.get(accountNumber.value());
        if (master == null) {
            return Optional.empty();
        }
        Map<Currency, Money> balances = new LinkedHashMap<>();
        for (FxSubAccountRow row : fxSubAccountTable.getOrDefault(master.accountNo, List.of())) {
            Currency currency = Currency.getInstance(row.currency);
            balances.put(currency, Money.of(row.balance, currency));
        }
        return Optional.of(ForeignCurrencyAccount.restore(
                new AccountNumber(master.accountNo),
                new CustomerId(master.customerId),
                ForeignCurrencyAccount.Status.valueOf(master.status),
                balances));
    }

    /** 給 Demo 檢視「表」內容用。 */
    public Map<String, List<FxSubAccountRow>> dumpSubAccountTable() {
        return fxSubAccountTable;
    }
}
