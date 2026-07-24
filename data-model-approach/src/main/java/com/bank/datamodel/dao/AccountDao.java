package com.bank.datamodel.dao;

import com.bank.datamodel.entity.AccountDO;

/** 單純的資料表存取介面（實作可為 MyBatis / JDBC / JPA）。 */
public interface AccountDao {

    AccountDO findByAccountNo(String accountNo);

    void update(AccountDO account);

    void insert(AccountDO account);
}
