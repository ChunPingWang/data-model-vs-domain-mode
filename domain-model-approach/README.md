# Domain Model 途徑（領域導向）

設計從 **業務概念** 出發：台幣帳戶、外幣綜合帳戶（一戶多幣）、信用卡
是三個獨立的聚合，各自守住自己的不變量；金額與幣別以 `Money` Value Object
綁定；跨聚合的流程（換匯、繳卡費）由 Domain Service 編排。

## 結構：六角架構（Ports & Adapters），domain 層單獨抽離

四個頂層套件的相依方向：`adapter → application → domain`，`bootstrap` 負責組裝。
**`domain/` 不 import 其他任何層**（也不 import 任何框架），是可以整包搬走的純業務核心。

| 套件 | 內容 | 構件 |
|---|---|---|
| `domain/model/shared/` | `Money`、`ExchangeRate` | Value Object |
| `domain/model/customer/` | `Customer`、`CustomerId`、`ContactInfo` | Aggregate Root / VO |
| `domain/model/account/` | `TwdAccount`、`ForeignCurrencyAccount`、`AccountNumber` | Aggregate Root / VO |
| `domain/model/card/` | `CreditCard`、`CardNumber` | Aggregate Root / VO |
| `domain/service/` | `CurrencyExchangeService`、`CreditCardPaymentService` | Domain Service |
| `domain/repository/` | 三個 Repository **介面**：**一個 Repository 對應一個聚合**，`save()` 傳入整個聚合 | Outbound Port |
| `application/port/in/` | `BuyForeignCurrencyUseCase`（介面，Command in / DTO out） | Inbound Port |
| `application/service/` | `BuyForeignCurrencyService`：輸入 Port 的實作，只編排 | Application Service |
| `adapter/in/web/` | `FxPurchaseController`：**只依賴輸入 Port 介面**，不認識實作與聚合 | Inbound Adapter |
| `adapter/out/persistence/` | Repository 實作：`ForeignCurrencyAccount` 聚合拆寫 `FX_ACCOUNT` + `FX_SUB_ACCOUNT` 兩張表 | Outbound Adapter |
| `bootstrap/` | `DomainModelDemo`：組裝根（唯一同時認識介面與實作的地方）＋可執行驗證 | Composition Root |
| `src/test/` | 中文 Gherkin（與 data 模組同一份）+ Cucumber Step Definitions | — |

```bash
mvn test -pl domain-model-approach        # 8 個 Cucumber 場景
java -cp target/classes com.bank.domainmodel.bootstrap.DomainModelDemo
```

## 關鍵設計決策

1. **不變量收進聚合**：「餘額不可為負」只存在於 `TwdAccount.withdraw()` 一處，
   任何呼叫端都繞不過去；物件不可能被外部改成非法狀態（沒有 setter）。
2. **Money 綁定幣別**：`TWD 100 + USD 100` 拋 `CurrencyMismatchException`——
   幣別錯帳從「上線後對帳發現」提前到「單元測試就爆」。
3. **一戶多幣是一級概念**：`ForeignCurrencyAccount` 內含 `Map<Currency, Money>` 子帳，
   而不是資料表裡的多列資料。
4. **聚合間以 ID 參照**：帳戶、卡片只記 `CustomerId`，不直接持有 `Customer` 物件，
   維持小聚合與獨立交易邊界（Vernon 聚合設計原則）。
5. **Domain Service 只編排**：`CurrencyExchangeService` 十行結束——
   換算歸 `ExchangeRate`、扣款歸 `TwdAccount`、入帳歸 `ForeignCurrencyAccount`。
6. **依賴反轉**：領域層定義 Repository 介面，不 import 任何 ORM/JDBC——
   資料存成幾張表是基礎設施層的實作細節。

## 純物件、零依賴，規則可以直接測

```java
TwdAccount acct = new TwdAccount(new AccountNumber("0123456789012"),
                                 new CustomerId("C000001"));
acct.deposit(Money.twd("1000"));
assertThrows(InsufficientBalanceException.class,
             () -> acct.withdraw(Money.twd("2000")));   // 不需要 mock 任何東西
```
