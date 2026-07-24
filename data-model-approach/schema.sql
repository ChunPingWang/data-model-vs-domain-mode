-- =====================================================================
-- Data Model 途徑：設計從「資料表」出發
-- 一張 ACCOUNT 表用 ACCT_TYPE 欄位同時涵蓋台幣與外幣帳戶，
-- 業務規則（哪種帳戶能做什麼）不在 schema 裡，全靠應用程式的 if/else。
-- =====================================================================

CREATE TABLE CUSTOMER (
    CUSTOMER_ID   VARCHAR(10)  PRIMARY KEY,          -- 客戶編號
    ID_NO         VARCHAR(10)  NOT NULL UNIQUE,      -- 身分證字號
    NAME          VARCHAR(60)  NOT NULL,
    PHONE         VARCHAR(20),
    EMAIL         VARCHAR(100),
    VIP_FLAG      CHAR(1)      DEFAULT 'N',          -- Y/N
    STATUS        CHAR(1)      DEFAULT 'A',          -- A:正常 S:停用 C:結清
    CREATED_AT    TIMESTAMP    NOT NULL
);

CREATE TABLE ACCOUNT (
    ACCOUNT_NO    VARCHAR(14)  PRIMARY KEY,
    CUSTOMER_ID   VARCHAR(10)  NOT NULL REFERENCES CUSTOMER(CUSTOMER_ID),
    ACCT_TYPE     CHAR(2)      NOT NULL,             -- 01:台幣活存 02:台幣定存 03:外幣活存
    CURRENCY      CHAR(3)      NOT NULL,             -- TWD / USD / JPY / EUR ...
    BALANCE       DECIMAL(18,2) NOT NULL DEFAULT 0,
    INTEREST_RATE DECIMAL(8,4),
    STATUS        CHAR(1)      DEFAULT 'A',          -- A:正常 F:凍結 C:已結清
    OPEN_DATE     DATE         NOT NULL
);

CREATE TABLE CREDIT_CARD (
    CARD_NO       VARCHAR(16)  PRIMARY KEY,
    CUSTOMER_ID   VARCHAR(10)  NOT NULL REFERENCES CUSTOMER(CUSTOMER_ID),
    CARD_TYPE     CHAR(2)      NOT NULL,             -- 01:普卡 02:金卡 03:白金卡
    CREDIT_LIMIT  DECIMAL(18,2) NOT NULL,            -- 信用額度
    USED_AMT      DECIMAL(18,2) NOT NULL DEFAULT 0,  -- 已用額度
    BILL_AMT      DECIMAL(18,2) NOT NULL DEFAULT 0,  -- 本期帳單金額
    STATUS        CHAR(1)      DEFAULT 'A',          -- A:正常 B:掛失 S:停卡
    EXPIRE_DATE   DATE         NOT NULL
);

CREATE TABLE TRANSACTION_LOG (
    TXN_ID        BIGINT       PRIMARY KEY,
    ACCOUNT_NO    VARCHAR(16)  NOT NULL,             -- 存款帳號或卡號共用此欄位
    TXN_TYPE      CHAR(2)      NOT NULL,             -- 01:存 02:提 03:轉 04:換匯 05:刷卡 06:繳款
    CURRENCY      CHAR(3)      NOT NULL,
    AMOUNT        DECIMAL(18,2) NOT NULL,
    EXCHANGE_RATE DECIMAL(12,6),                     -- 換匯交易才有值
    MEMO          VARCHAR(200),
    TXN_TIME      TIMESTAMP    NOT NULL
);
