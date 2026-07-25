package com.bank.datamodel.dao;

import com.bank.datamodel.entity.AccountDO;

import java.util.HashMap;
import java.util.Map;

/** 以 Map 模擬 ACCOUNT 表：一個 DAO 對應一張表，傳輸單位是「一列」。 */
public class InMemoryAccountDao implements AccountDao {

    private final Map<String, AccountDO> accountTable = new HashMap<>();

    @Override
    public AccountDO findByAccountNo(String accountNo) {
        return accountTable.get(accountNo);
    }

    @Override
    public void update(AccountDO account) {
        accountTable.put(account.getAccountNo(), account);
    }

    @Override
    public void insert(AccountDO account) {
        accountTable.put(account.getAccountNo(), account);
    }
}
