# Data Model 途徑（資料導向）

設計從 **資料表** 出發：先畫 ER 圖決定 `CUSTOMER / ACCOUNT / CREDIT_CARD / TRANSACTION_LOG`
四張表，程式物件（`*DO`）是資料表的一對一鏡射，業務邏輯以
**Transaction Script**（Fowler）的形式集中在 Service 實作。

## 結構：三層式架構（SOLID 版）

類別相依全部介面化：Controller → Service 介面 → DAO 介面（DIP），
一個業務域一個 Service 介面（SRP/ISP）。
但 `AccountDO` 仍貫穿三層、外洩到 API——那是 Data Model 範式的本質，不是 SOLID 能解的。

| 層 | 內容 |
|---|---|
| `schema.sql` | 資料表定義——真正的「模型」在這裡 |
| `web/` | Presentation：`AccountController` 只依賴 Service **介面**；DO 原樣回傳前端，API 契約 = 資料表 schema |
| `service/` | Business 抽象：`AccountService` / `FxService` / `CreditCardService` 三個介面 |
| `service/impl/` | Transaction Script 實作：存款、提款、換匯、刷卡、繳款（規則全在這） |
| `dao/` | Data Access：**一個 DAO 介面對應一張表**，傳輸單位是「一列 DO」（含 in-memory 實作） |
| `entity/` | `CustomerDO` / `AccountDO` / `CreditCardDO`：只有 getter/setter 的貧血物件 |
| `demo/` | `DataModelDemo`：組裝根＋可執行驗證（含資料洞示範） |
| `src/test/` | 中文 Gherkin（與 domain 模組同一份）+ Cucumber Step Definitions |

```bash
mvn test -pl data-model-approach          # 8 個 Cucumber 場景
java -cp target/classes com.bank.datamodel.demo.DataModelDemo
```

## 這個寫法的代價（對照 domain-model-approach 逐條驗證）

1. **規則重複**：`deposit` 與 `withdraw` 各自檢查帳戶狀態；漏掉一處就是資料洞。
2. **不變量無保護**：任何程式拿到 `AccountDO` 都能 `setBalance(負數)`。
3. **型別不設防**：帳號、卡號都是 `String`，金額是裸 `BigDecimal`，
   幣別是另一個獨立欄位——美元加進台幣餘額，編譯器毫無反應。
4. **修改放大**：新增帳戶類型 `ACCT_TYPE=04` 時，要全域搜尋所有判斷
   `acctType` 的 if/else 逐一補分支。
5. **測試昂貴**：規則長在 Service 與 DAO 之間，測一條「餘額不足」規則也得 mock 資料庫。

## 它仍然是對的選擇，當——

- 功能以 CRUD 與查詢為主，規則稀薄（客戶資料維護、報表）
- 批次做集合運算（日終計息、對帳），SQL 直取比逐物件處理快得多
