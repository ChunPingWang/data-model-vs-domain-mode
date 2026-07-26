# Data Model vs. Domain Model — 以銀行客戶服務為例

以同一組銀行業務（**台幣帳戶、外幣帳戶、信用卡**）為標的，
用兩套**可執行、可測試**的 Java 程式碼，比較「資料導向（Data Model）」與「領域導向（Domain Model）」
兩種建模方式在 **程式架構** 與 **資料設計** 上的差異。

兩個模組共用**同一份中文 Gherkin 業務場景**（BDD），各自用 Cucumber 驗收自己的實作
——測試本身也是比較的一部分。

```
├── pom.xml                          # Maven 多模組（Cucumber + JUnit 5）
├── data-model-approach/             # 資料導向：三層式架構（SOLID 版）
│   ├── schema.sql                   #   設計起點：ER 圖與資料表
│   └── src
│       ├── main/.../datamodel/
│       │   ├── web/                 # Presentation：Controller 只依賴 Service「介面」
│       │   ├── service/             # Business 抽象：AccountService / FxService / CreditCardService
│       │   │   └── impl/            #   Transaction Script 實作（規則全在這）
│       │   ├── dao/                 # Data Access：一個 DAO 介面對應一張表，傳輸單位是「一列」
│       │   ├── entity/              # 貧血 DO（只有 getter/setter，貫穿三層）
│       │   └── demo/                # DataModelDemo —— 組裝根 + 可執行驗證
│       └── test/
│           ├── resources/features/  # 中文 Gherkin（與另一模組同一份）
│           └── java/.../bdd/        # Cucumber Step Definitions
└── domain-model-approach/           # 領域導向：六角架構（Ports & Adapters）
    └── src
        ├── main/.../domainmodel/
        │   ├── domain/              # ★ 純領域層，單獨抽離，不依賴其他任何層
        │   │   ├── model/
        │   │   │   ├── shared/      #   Money / ExchangeRate（Value Object）
        │   │   │   ├── customer/    #   Customer 聚合
        │   │   │   ├── account/     #   TwdAccount / ForeignCurrencyAccount 聚合
        │   │   │   └── card/        #   CreditCard 聚合
        │   │   ├── service/         #   跨聚合的 Domain Service
        │   │   └── repository/      #   輸出 Port：一個 Repository 介面對應「一個聚合」
        │   ├── application/
        │   │   ├── port/in/         #   輸入 Port：BuyForeignCurrencyUseCase（介面）
        │   │   └── service/         #   BuyForeignCurrencyService（Port 實作，只編排）
        │   ├── adapter/
        │   │   ├── in/web/          #   FxPurchaseController —— 只依賴輸入 Port 介面
        │   │   └── out/persistence/ #   Repository 實作：一個聚合 ↔ 多張表
        │   └── bootstrap/           #   組裝根（Composition Root）+ 可執行驗證
        └── test/                    # 同一份 Gherkin + 自己的 Step Definitions
```

## 如何使用

**環境需求**：JDK 21+、Maven 3.9+（不需要資料庫，儲存層以 in-memory 實作模擬）。

```bash
# 1. 跑 BDD 驗收測試：兩個模組各 8 個 Cucumber 場景（同一份 Gherkin）
mvn test

# 只跑單一模組
mvn test -pl data-model-approach
mvn test -pl domain-model-approach

# 2. 跑端到端 Demo（先 mvn test 或 mvn compile 產出 target/classes）
java -cp data-model-approach/target/classes   com.bank.datamodel.demo.DataModelDemo
java -cp domain-model-approach/target/classes com.bank.domainmodel.bootstrap.DomainModelDemo
```

**建議閱讀順序**：

1. 先讀兩邊的「模型」：`schema.sql`（Data Model 的起點）↔ `domain/model/`（Domain Model 的起點）。
2. 讀同一個業務場景的測試：`src/test/resources/features/currency_exchange.feature`（兩模組同一份），
   再對照兩邊的 Step Definitions（`DataModelSteps` ↔ `DomainModelSteps`）。
3. 順著一筆換匯交易走完各自的分層（見第三節的循序圖與 Class Diagram）。
4. 跑兩支 Demo，看 Data Model 側的「資料洞示範」與 Domain Model 側的「不變量驗證」輸出。

**如何在此架構上加新功能**（以「新增『台幣轉帳』」為例）：

| 步驟 | data-model-approach（三層式） | domain-model-approach（六角） |
|---|---|---|
| 1 | 在 `features/` 寫 Gherkin 場景 | 同左（同一份） |
| 2 | `service/` 加 `TransferService` 介面 | `domain/model/account/` 為 `TwdAccount` 加 `transferTo()` 行為（或不用改） |
| 3 | `service/impl/` 寫 Transaction Script | `application/port/in/` 加 `TransferUseCase` 介面；`application/service/` 實作編排 |
| 4 | Controller 注入新介面 | `adapter/in/web/` 的 Controller 注入新輸入 Port |
| 5 | 補 Step Definitions → `mvn test` | 同左 |

---

## 一、兩種模型的出發點

| | Data Model（資料模型） | Domain Model（領域模型） |
|---|---|---|
| 回答的問題 | 資料**長什麼樣、怎麼存**？ | 業務**是什麼、怎麼運作**？ |
| 設計起點 | ER 圖、資料表、欄位、正規化 | 業務概念、規則、通用語言（Ubiquitous Language） |
| 物件的角色 | 資料表的鏡射（DO/PO），只有 getter/setter | 業務概念的化身，有行為、守規則 |
| 業務邏輯位置 | 集中在 Service（Transaction Script） | 分佈在聚合、Value Object、Domain Service |
| 一句話 | **表驅動程式** | **業務驅動程式** |

出發點不同，但**設計結果會體現在每一層**。第三節以同一個使用情境把兩邊的三層全部攤開對照。

---

## 二、資料設計比較

### Data Model：一張 ACCOUNT 表打天下

```mermaid
erDiagram
    CUSTOMER ||--o{ ACCOUNT : "1:N"
    CUSTOMER ||--o{ CREDIT_CARD : "1:N"
    ACCOUNT ||--o{ TRANSACTION_LOG : "1:N"
    CUSTOMER {
        varchar CUSTOMER_ID PK
        varchar ID_NO
        char VIP_FLAG "Y/N"
        char STATUS "A/S/C"
    }
    ACCOUNT {
        varchar ACCOUNT_NO PK
        char ACCT_TYPE "01台幣活存 02台幣定存 03外幣"
        char CURRENCY "TWD/USD/JPY"
        decimal BALANCE
        char STATUS "A/F/C"
    }
    CREDIT_CARD {
        varchar CARD_NO PK
        decimal CREDIT_LIMIT
        decimal USED_AMT
        decimal BILL_AMT
        char STATUS "A/B/S"
    }
```

特徵（見 [`data-model-approach/schema.sql`](data-model-approach/schema.sql)）：

- 台幣與外幣帳戶**共用一張表**，靠 `ACCT_TYPE` + `CURRENCY` 欄位碼區分。
- 客戶「一戶多幣」的外幣綜合帳戶，只能拆成多列資料，概念在 schema 上消失。
- 金額（`BALANCE`）與幣別（`CURRENCY`）是兩個**互不相干的欄位**，資料庫不會阻止你把美元加進台幣餘額。
- 「可用額度 = CREDIT_LIMIT − USED_AMT」是隱含知識，表上看不出來。
- 業務規則（定存不能直接提領、外幣帳戶不收台幣）**完全不在資料層**，全靠應用程式自律。

### Domain Model：概念各自成型，資料是實作細節

```mermaid
classDiagram
    class Customer {
        <<Aggregate Root>>
        CustomerId id
        upgradeToVip()
        suspend()
        canOpenNewAccount() bool
    }
    class TwdAccount {
        <<Aggregate Root>>
        Money balance
        deposit(Money)
        withdraw(Money)
        freeze()
    }
    class ForeignCurrencyAccount {
        <<Aggregate Root>>
        Map~Currency,Money~ subAccounts
        deposit(Money)
        withdraw(Money)
        balanceOf(Currency) Money
    }
    class CreditCard {
        <<Aggregate Root>>
        Money creditLimit
        Money usedCredit
        authorize(Money, YearMonth)
        applyPayment(Money)
        availableCredit() Money
    }
    class Money {
        <<Value Object>>
        BigDecimal amount
        Currency currency
        add(Money) Money
        subtract(Money) Money
    }
    class ExchangeRate {
        <<Value Object>>
        convert(Money) Money
    }
    class CurrencyExchangeService {
        <<Domain Service>>
        buyForeignCurrency(from, to, twd, rate)
    }
    TwdAccount ..> Customer : ownerId (以 ID 參照)
    ForeignCurrencyAccount ..> Customer : ownerId
    CreditCard ..> Customer : holderId
    TwdAccount --> Money
    ForeignCurrencyAccount --> Money
    CreditCard --> Money
    CurrencyExchangeService ..> TwdAccount
    CurrencyExchangeService ..> ForeignCurrencyAccount
    CurrencyExchangeService ..> ExchangeRate
```

特徵：

- 台幣帳戶、外幣帳戶、信用卡是**三個獨立的聚合（型別）**，不是同一張表的三種碼值。
- `Money` 把金額與幣別鎖在一起：`TWD 100 + USD 100` 直接拋 `CurrencyMismatchException`。
- 外幣帳戶「一戶多幣」是模型的第一級概念（`Map<Currency, Money>` 子帳）。
- 「可用額度」是**行為**（`availableCredit()`），定義只寫一次。
- 聚合之間只以 ID 相連（`CustomerId`），維持各自的交易邊界。
- 資料怎麼存由基礎設施層的 Repository 實作決定，領域層不知情——**依賴方向反轉**。

---

## 三、同一個使用情境，走完三層：前端 → Business → Repository

使用情境：**客戶用台幣帳戶 32,500 元，以匯率 32.5 結購美元 1,000 元存入外幣帳戶。**

### Data Model 途徑的三層

```mermaid
sequenceDiagram
    participant UI as 前端
    participant C as AccountController<br/>(只依賴 Service 介面)
    participant S as FxServiceImpl<br/>(Transaction Script)
    participant D as AccountDao<br/>(介面，一表一 DAO)
    UI->>C: POST /fx/purchase (帳號、金額、匯率都是裸字串/數字)
    C->>S: buyForeignCurrency(...) — 透過 FxService 介面
    S->>D: findByAccountNo(台幣帳號) → AccountDO(一列)
    S->>D: findByAccountNo(外幣帳號) → AccountDO(一列)
    Note over S: 所有 if/else 檢查 + 匯率計算<br/>+ setBalance() 都在這裡
    S->>D: update(台幣列)
    S->>D: update(外幣列)
    UI->>C: GET /accounts/{no}
    C-->>UI: AccountDO 原樣序列化<br/>{acctType:"03", status:"A", ...} 碼值由前端翻譯
```

### Domain Model 途徑的三層

```mermaid
sequenceDiagram
    participant UI as 前端
    participant C as FxPurchaseController<br/>(Web Adapter，只依賴輸入 Port 介面)
    participant U as BuyForeignCurrencyService<br/>(輸入 Port 的實作)
    participant A as TwdAccount / FxAccount<br/>(聚合) + ExchangeRate(VO)
    participant R as Repository<br/>(輸出 Port 介面，一聚合一 Repository)
    UI->>C: POST /fx/purchase
    C->>U: execute(Command) — 透過介面呼叫
    U->>R: findByAccountNumber() → 完整聚合
    U->>A: rate.convert() / from.withdraw() / to.deposit()
    Note over A: 規則在模型裡：<br/>餘額不足、幣別不符、凍結 → 聚合自己擋
    U->>R: save(整個聚合)
    Note over R: 內部拆寫 FX_ACCOUNT 主檔<br/>+ FX_SUB_ACCOUNT 每幣別一列
    U-->>C: Result DTO
    C-->>UI: Result DTO（語意化欄位，與表結構脫鉤）
```

### Class Diagram：兩邊都遵守 SOLID，分水嶺在「模型」

兩個途徑的類別相依**都已介面化**（Controller 依賴 Service 介面、實作依賴 DAO／Repository 介面）。
真正的差異在**每一層傳遞的東西**：Data Model 是同一顆 `AccountDO` 貫穿三層；
Domain Model 是每層有自己的模型（Command/DTO ↔ 聚合 ↔ Row）。

**Data Model 途徑（三層式，SOLID 版）**——介面都對了，
但 `AccountDO`（＝ACCOUNT 表的鏡射）仍然貫穿三層、外洩到 API 契約：

```mermaid
classDiagram
    direction TB
    class AccountController {
        <<Presentation>>
        +getAccount(no) AccountDO
        +buyForeignCurrency(...)
    }
    class AccountService {
        <<Business · interface>>
        +getAccount(no) AccountDO
        +deposit() / withdraw()
    }
    class FxService {
        <<Business · interface>>
        +buyForeignCurrency(...)
    }
    class AccountServiceImpl {
        <<Transaction Script>>
    }
    class FxServiceImpl {
        <<Transaction Script>>
    }
    class AccountDao {
        <<Data Access · interface>>
        +findByAccountNo(String) AccountDO
        +update(AccountDO)
    }
    class InMemoryAccountDao {
        <<Persistence>>
    }
    class AccountDO {
        <<ACCOUNT 表鏡射>>
        +getBalance() / setBalance()
        +getAcctType() : "01"/"02"/"03"
    }
    AccountController ..> AccountService : 依賴介面 (DIP)
    AccountController ..> FxService : 依賴介面 (DIP)
    AccountService <|.. AccountServiceImpl
    FxService <|.. FxServiceImpl
    AccountServiceImpl ..> AccountDao : 依賴介面 (DIP)
    FxServiceImpl ..> AccountDao : 依賴介面 (DIP)
    AccountDao <|.. InMemoryAccountDao
    AccountController --> AccountDO : ★ DO 外洩到 API
    AccountServiceImpl --> AccountDO : ★ 直接 get/set 欄位
    InMemoryAccountDao --> AccountDO : ★ 同一顆 DO 貫穿三層
```

**Domain Model 途徑（嚴格 SOLID）**——前端 Adapter 與 Repository 實作**都指向核心**：
Web 依賴輸入 Port「介面」，Infrastructure 實作輸出 Port「介面」；
核心（application + domain）不 import 任何 Adapter，兩端可獨立抽換：

```mermaid
classDiagram
    direction TB
    class FxPurchaseController {
        <<Web Adapter>>
        -useCase : BuyForeignCurrencyUseCase
        +purchase(...) Result
    }
    class BuyForeignCurrencyUseCase {
        <<Inbound Port · interface>>
        +execute(Command) Result
    }
    class BuyForeignCurrencyService {
        <<Application Service>>
        +execute(Command) Result
    }
    class CurrencyExchangeService {
        <<Domain Service>>
        +buyForeignCurrency(from, to, twd, rate) Money
    }
    class TwdAccount {
        <<Aggregate Root>>
        +withdraw(Money)
    }
    class ForeignCurrencyAccount {
        <<Aggregate Root>>
        +deposit(Money)
    }
    class TwdAccountRepository {
        <<Outbound Port · interface>>
        +findByAccountNumber(..) TwdAccount
        +save(TwdAccount)
    }
    class ForeignCurrencyAccountRepository {
        <<Outbound Port · interface>>
        +save(ForeignCurrencyAccount)
    }
    class InMemoryTwdAccountRepository {
        <<Persistence Adapter>>
    }
    class InMemoryForeignCurrencyAccountRepository {
        <<Persistence Adapter>>
        FX_ACCOUNT + FX_SUB_ACCOUNT
    }
    FxPurchaseController ..> BuyForeignCurrencyUseCase : 只依賴介面 (DIP)
    BuyForeignCurrencyUseCase <|.. BuyForeignCurrencyService : 實作
    BuyForeignCurrencyService ..> TwdAccountRepository : 只依賴介面
    BuyForeignCurrencyService ..> ForeignCurrencyAccountRepository : 只依賴介面
    BuyForeignCurrencyService ..> CurrencyExchangeService : 編排
    CurrencyExchangeService ..> TwdAccount
    CurrencyExchangeService ..> ForeignCurrencyAccount
    TwdAccountRepository <|.. InMemoryTwdAccountRepository : 實作 (DIP)
    ForeignCurrencyAccountRepository <|.. InMemoryForeignCurrencyAccountRepository : 實作 (DIP)
    TwdAccountRepository ..> TwdAccount : save(聚合)
    ForeignCurrencyAccountRepository ..> ForeignCurrencyAccount : save(聚合)
```

實際 import 驗證（程式碼與圖一致）：

- [`FxPurchaseController`](domain-model-approach/src/main/java/com/bank/domainmodel/adapter/in/web/FxPurchaseController.java) 只 import `application.port.in.BuyForeignCurrencyUseCase`——不認識 Service 實作、聚合、Repository。
- [`InMemoryForeignCurrencyAccountRepository`](domain-model-approach/src/main/java/com/bank/domainmodel/adapter/out/persistence/InMemoryForeignCurrencyAccountRepository.java) 只 import 領域層（Repository 介面＋聚合）——不認識 application 與 web。
- 介面與實作只在**組裝根**（[`DomainModelDemo`](domain-model-approach/src/main/java/com/bank/domainmodel/bootstrap/DomainModelDemo.java) 的 main、或 Spring 的 DI 容器）相遇。

### SOLID 逐條對應：兩邊怎麼各自滿足，以及 SOLID 修不了什麼

| 原則 | Data Model（三層式作法） | Domain Model（六角作法） |
|---|---|---|
| **S**RP | 上帝類別拆成 `AccountService` / `FxService` / `CreditCardService`，一個業務域一個類別 | Controller 只轉譯、UseCase 只編排、聚合只守自己的規則、Repository 實作只管存取 |
| **O**CP | 加新「業務」＝加新 Service 介面＋實作，既有類別不動 | 加新「業務型態」＝新增聚合＋新 Port，既有類別不動 |
| **L**SP | 任何 `AccountDao` 實作（in-memory/JDBC/MyBatis）可互換，Service 行為不變 | 任何 Repository 實作可互換，UseCase 行為不變——BDD 測試就是替換證明 |
| **I**SP | 一個業務域一個小介面，Controller 只注入用得到的 | 一個 Use Case 一個輸入 Port；一個聚合一個輸出 Port |
| **D**IP | Controller → Service **介面**；ServiceImpl → DAO **介面** | 前端 Adapter → 輸入 Port 介面；Repository 實作 → 輸出 Port 介面，箭頭全部指向核心 |

> **SOLID 是類別關係的紀律，不是建模範式**——兩邊都能合規。合規之後仍然消不掉的差異，才是
> Data Model vs. Domain Model 的本質：
> 1. **模型貫穿 vs. 每層一模**：`AccountDO` 從 DAO 一路裸奔到 API；六角側是 Command/DTO ↔ 聚合 ↔ Row，各層解耦。
> 2. **規則的落點**：三層式規則仍在 Service 的 if/else（`OCP` 只保護「加業務」，「加帳戶型態 `ACCT_TYPE=04`」
>    還是得回頭改 `AccountServiceImpl` 的分支）；六角側加型態＝加聚合。
> 3. **不變量保護**：`AccountDO.setBalance(-50000)` 依然任何人都寫得出來（Demo 有實證）；聚合側沒有 setter。
> 4. **Repository 粒度**：DAO ↔ 表（傳一列）；Repository ↔ 聚合（傳整個聚合，表數是實作細節）。

### 逐層對照（每一格都有對應的程式檔可驗證）

| 層 | Data Model 途徑 | Domain Model 途徑 |
|---|---|---|
| **前端呈現** | [`AccountController`](data-model-approach/src/main/java/com/bank/datamodel/web/AccountController.java) 依賴 Service **介面**（DIP），但把 `AccountDO` 原樣回傳：畫面拿到 `acctType:"03"`、`status:"A"`，自己查碼表翻譯。**API 契約 = 資料表 schema**，改表即改 API | [`FxPurchaseController`](domain-model-approach/src/main/java/com/bank/domainmodel/adapter/in/web/FxPurchaseController.java) 只依賴[輸入 Port 介面](domain-model-approach/src/main/java/com/bank/domainmodel/application/port/in/BuyForeignCurrencyUseCase.java)，回傳語意化 DTO（`purchasedFxAmount: "USD 1000.00"`）。聚合不外洩，**API 契約與儲存結構脫鉤** |
| **Business Layer** | [`FxServiceImpl`](data-model-approach/src/main/java/com/bank/datamodel/service/impl/FxServiceImpl.java) 等 Transaction Script 做完載入、檢查、計算、寫回；同類檢查跨 Service 重複 | [`BuyForeignCurrencyService`](domain-model-approach/src/main/java/com/bank/domainmodel/application/service/BuyForeignCurrencyService.java) 實作輸入 Port、只編排（載入→呼叫領域行為→存回→轉 DTO）；規則在聚合與 `ExchangeRate` 裡，各寫一次 |
| **Repository Layer** | **一個 DAO ↔ 一張表**，傳輸單位是「一列 DO」；跨表一致性由 Service 呼叫兩次 `update()` 自行維持 | **一個 Repository ↔ 一個聚合**，`save()` 傳入**整個聚合**；[`InMemoryForeignCurrencyAccountRepository`](domain-model-approach/src/main/java/com/bank/domainmodel/adapter/out/persistence/InMemoryForeignCurrencyAccountRepository.java) 內部拆寫 `FX_ACCOUNT` + `FX_SUB_ACCOUNT` 兩張表，呼叫端毫不知情 |

> **Repository 粒度是兩者最本質的差異之一**：
> Data Model 的 DAO 與資料表一一對應（`AccountDao ↔ ACCOUNT`），聚合這個概念不存在；
> Domain Model 的 Repository 與**聚合**一一對應，聚合對應幾張表是基礎設施的私事——
> `TwdAccount` 剛好一張表、`ForeignCurrencyAccount` 是主檔+子帳兩張表，
> 但兩個 Repository 的契約長得一模一樣：`save(聚合)` / `findBy...() → 聚合`。
> 交易與一致性邊界因此從「Service 方法」移到「聚合」身上。

兩邊各附一支可執行 Demo 驗證上述行為（含 Data Model 途徑「繞過 Service 寫出負餘額、幣別錯帳」的資料洞示範）：
[`DataModelDemo`](data-model-approach/src/main/java/com/bank/datamodel/demo/DataModelDemo.java)、
[`DomainModelDemo`](domain-model-approach/src/main/java/com/bank/domainmodel/bootstrap/DomainModelDemo.java)。

---

## 四、從測試出發：Gherkin → Cucumber → prod. code

BDD 的走法：先用業務語言寫**測試案例**（Gherkin），再寫**測試程式**（Cucumber Step Definitions），
最後讓 **prod. code** 通過驗收。本專案的關鍵設計：**兩個模組共用同一份 Gherkin**——
業務場景與實作方式無關，但 Step Definitions 的長相立刻暴露兩種架構的差異。

### 測試案例（節錄，[完整 feature 檔](domain-model-approach/src/test/resources/features/)）

```gherkin
# language: zh-TW
功能: 台幣結購外幣（換匯）

  場景: 餘額足夠時結購成功
    假設 客戶 "C000001" 的台幣帳戶 "0011223344556" 餘額為 100000 元
    並且 客戶 "C000001" 擁有外幣帳戶 "0099887766554"
    當 以匯率 32.5 用台幣 32500 元結購美元
    那麼 交易成功
    並且 台幣帳戶餘額應為 67500 元
    並且 外幣帳戶的美元子帳餘額應為 1000 美元

  場景: 台幣餘額不足時拒絕交易
    假設 客戶 "C000001" 的台幣帳戶 "0011223344556" 餘額為 10000 元
    並且 客戶 "C000001" 擁有外幣帳戶 "0099887766554"
    當 以匯率 32.5 用台幣 32500 元結購美元
    那麼 交易應被拒絕並提示 "餘額不足"
    並且 台幣帳戶餘額應為 10000 元
```

共 8 個場景 × 2 個模組（換匯 2、台幣存提 3、信用卡授權 3），`mvn test` 全數通過：

```
Tests run: 8, Failures: 0 -- in com.bank.datamodel.bdd.CucumberTest
Tests run: 8, Failures: 0 -- in com.bank.domainmodel.bdd.CucumberTest
```

### 同一步驟，兩種 Step Definition

「假設 客戶持有信用卡 額度 50000 元」這一步——

[Data Model 側](data-model-approach/src/test/java/com/bank/datamodel/bdd/DataModelSteps.java)：
測試被迫講「資料表方言」，手工組欄位碼；規則要經過 Service + DAO 才測得到。

```java
@Given("客戶 {string} 持有信用卡 {string} 額度 {int} 元")
public void 建立信用卡(String customerId, String cardNo, int limit) {
    CreditCardDO row = new CreditCardDO();
    row.setCardNo(cardNo);
    row.setCustomerId(customerId);
    row.setCardType("02");                      // 欄位碼：測試也得知道 02 是金卡
    row.setCreditLimit(new BigDecimal(limit));
    row.setUsedAmt(BigDecimal.ZERO);            // 漏設任何一欄，測的就不是真實狀態
    row.setBillAmt(BigDecimal.ZERO);
    row.setStatus("A");
    row.setExpireDate(LocalDate.of(2030, 12, 31));
    cardDao.insert(row);
}
```

[Domain Model 側](domain-model-approach/src/test/java/com/bank/domainmodel/bdd/DomainModelSteps.java)：
測試講的語言就是 prod. code 的語言，Gherkin 一句對應一個領域行為。

```java
@Given("客戶 {string} 持有信用卡 {string} 額度 {int} 元")
public void 建立信用卡(String customerId, String cardNo, int limit) {
    card = new CreditCard(new CardNumber(cardNo), new CustomerId(customerId),
            Money.twd(String.valueOf(limit)), YearMonth.of(2030, 12));
}

@Given("該卡已掛失")
public void 掛失() {
    card.reportLost();          // 對照組：Data Model 側是 row.setStatus("B")
}
```

| 測試面向 | Data Model | Domain Model |
|---|---|---|
| Given 的準備工作 | 組資料列、填欄位碼（`"02"`、`"A"`、`"B"`） | `new` 聚合、呼叫領域行為（`reportLost()`） |
| 規則的受測單位 | 只能整條 Service 流程一起測 | 聚合可單獨測，UseCase 另測編排 |
| 測試與業務語言的距離 | Gherkin 說「掛失」，程式寫 `setStatus("B")` | Gherkin 說「掛失」，程式寫 `reportLost()` |
| 需要的基礎設施 | DAO（真 DB 或 mock/in-memory） | 純物件即可；Repository 只在 UseCase 級測試出現 |

---

## 五、架構特性對照

| 面向 | Data Model 途徑 | Domain Model 途徑 |
|---|---|---|
| 分層 | Controller → Service → DAO → Table | Interface → Application → **Domain** → Infrastructure |
| 業務規則落點 | Service 方法內的 if/else，**同一規則多處重複**（deposit/withdraw 各檢查一次狀態） | 聚合唯一入口，**規則只寫一次**，呼叫端繞不過 |
| 不變量保護 | 靠工程師記得檢查；任何人 `setBalance()` 就能改出負餘額（Demo 有實證） | 建構子 + 行為方法把關；物件**不可能**進入非法狀態（沒有 setter） |
| 型別安全 | `String acctType`、裸 `BigDecimal`：把卡號當帳號傳、美元加台幣，編譯期無感 | `AccountNumber`／`CardNumber`／`Money`：這類錯誤**編譯不過或立刻拋例外** |
| 新業務型態（例：加開數位帳戶） | ACCOUNT 加一個 `ACCT_TYPE=04`，然後**全域搜尋每個 if/else** 補分支 | 新增一個 `DigitalAccount` 聚合，既有程式碼不動（開放封閉） |
| 併發控制粒度 | 以「資料列」思考，容易整批鎖表 | 以「聚合」為交易邊界，一次交易鎖一個聚合 |
| 團隊溝通 | 「把 ACCT_TYPE 03 的 BALANCE 減掉再 update」 | 「外幣帳戶提領美元子帳」——與業務單位**同一種語言** |

### 資料↔物件的對應關係

| | Data Model | Domain Model |
|---|---|---|
| 物件:資料表 | 1:1（AccountDO ↔ ACCOUNT） | 不必 1:1（FX 聚合 ↔ 主檔+子帳兩張表；`Money` 攤平成兩欄） |
| Repository/DAO 粒度 | **一個 DAO ↔ 一張表**，傳「一列」 | **一個 Repository ↔ 一個聚合**，傳「整個聚合」 |
| 依賴方向 | 程式依賴資料表結構，**改表就改程式** | 領域層定義 Repository 介面，**儲存方式可抽換** |
| 衍生值 | 存欄位或每處重算（`USED_AMT`、可用額度） | 行為即定義（`availableCredit()`） |
| 狀態表達 | 魔術碼 `'A'/'F'/'C'` 散落各處 | `enum Status { ACTIVE, FROZEN, CLOSED }` |

---

## 六、什麼時候該用哪一種？

**適合 Data Model（Transaction Script）：**

- CRUD 為主、規則稀薄的功能：客戶基本資料維護、參數檔、報表查詢
- 批次與資料整併：日終計息、對帳、餘額匯總——本質就是集合運算，SQL 比物件快
- 查詢端（CQRS 的 Q 端）：畫面要的是扁平資料，不需要行為

**適合 Domain Model：**

- 規則密集、狀態機複雜的核心業務：授信審核、信用卡授權、換匯、風控
- 規則會持續演進的領域：今天「VIP 免手續費」、明天「數位帳戶限額」
- 不變量代價高的場景：餘額為負、超額授權——出錯就是**金錢損失**

**實務上的常見組合（同一套核心系統內並存）：**

| 業務 | 建議 | 理由 |
|---|---|---|
| 台幣/外幣「交易」（存提、換匯） | Domain Model | 不變量密集，出錯即損失 |
| 信用卡「授權/額度」 | Domain Model | 狀態機 + 額度規則持續變動 |
| 帳務「查詢/報表/對帳單」 | Data Model | 扁平讀取，SQL 直達最有效率 |
| 日終「批次計息」 | Data Model | 集合運算，逐物件處理反而慢 |

> 關鍵不是「哪個比較先進」，而是：**寫入端的複雜規則交給 Domain Model 守護，
> 讀取端與批次的大量資料交給 Data Model 直取**——這正是 CQRS 的精神。

### 「並存」的落地：程式碼分路徑，資料表怎麼應對？

「並存」指的是**程式碼層面**：同一套核心系統、通常同一個資料庫，
依用途分成不同的**存取路徑**，而不是拆成兩個系統：

```mermaid
flowchart LR
    subgraph CODE[同一套核心系統：三條存取路徑]
        W["寫入路徑（交易）<br/>Controller → UseCase → 聚合 → Repository"]
        Q["查詢路徑（報表/畫面）<br/>Query Service → DAO/SQL → 扁平 DTO"]
        B["批次路徑（計息/對帳）<br/>SQL 集合運算"]
    end
    subgraph DB[同一個資料庫]
        WT[("寫入模型表<br/>TWD_ACCOUNT<br/>FX_ACCOUNT + FX_SUB_ACCOUNT<br/>CREDIT_CARD")]
        V[("DB View / 讀模型表<br/>V_CUSTOMER_POSITION")]
    end
    W -->|"強一致交易（鎖一個聚合）"| WT
    Q --> V
    V -."SQL JOIN（同庫）或事件同步（異庫）".- WT
    B -->|"直接集合運算，事後對帳把關"| WT
```

- **寫入路徑**走 Domain Model：聚合守不變量，Repository 把聚合落到「寫入模型表」。
- **查詢路徑**走 Data Model：Query Service 用 SQL/DAO 直接讀表、回傳扁平 DTO，
  **不經過聚合**——畫面要的是資料形狀，載入整個聚合只是浪費。
- **批次路徑**也走 Data Model：日終計息、對帳本質是集合運算，SQL 直接對表最快。

**資料表的三種應對方式**（由簡到繁，多數系統停在第 1 種就夠）：

| 方式 | 表結構 | 一致性 | 適用 |
|---|---|---|---|
| **1. 共用同一套表**（CQRS-lite，預設） | 表跟著**寫入模型**設計（聚合形狀，如 `FX_ACCOUNT`+`FX_SUB_ACCOUNT`）；查詢端用 SQL JOIN 或 **DB View** 攤平成報表形狀 | 同庫同交易，**強一致** | 絕大多數核心系統 |
| **2. 另建讀模型表**（完整 CQRS） | 寫入表（聚合形狀）＋讀取表（查詢形狀、反正規化），以**領域事件／CDC／批次 ETL** 同步 | **最終一致**（有同步延遲） | 查詢量大、查詢形狀與寫入模型差很遠（客戶 360、跨系統彙總） |
| **3. 批次直接操作寫入表** | 不另建表，SQL 集合運算直接更新 | 繞過了聚合，不變量**改由批次規則＋事後對帳**把關 | 日終計息、沖正、整批調帳 |

對應到本 repo：`InMemoryForeignCurrencyAccountRepository` 落地的
`FX_ACCOUNT`＋`FX_SUB_ACCOUNT` 就是「寫入模型表」；若要做「客戶總覽」報表，
正確做法**不是**載入三個聚合再組裝，而是直接
`SELECT ... FROM TWD_ACCOUNT JOIN FX_SUB_ACCOUNT JOIN CREDIT_CARD`（或建一個 View）
回傳扁平 DTO——這條查詢路徑長得就像 data-model-approach 的寫法，而這**完全沒問題**，
因為它只讀不寫，不需要不變量保護。

兩個提醒：
1. **表結構的主人是寫入模型**。表為聚合而設計，查詢用 View/JOIN 去遷就；
   反過來讓報表需求扭曲寫入表，聚合↔表的對應就會開始腐化。
2. **批次繞過聚合是刻意的破例**，不是免費的——第 3 種方式等於暫時放棄模型保護，
   所以銀行實務上批次一定伴隨事後對帳（reconciliation）補上驗證。

### 讀模型同步選型：CQRS？CDC？還是更簡單的方案？

當第 1 種（共用表＋View）撐不住時，才需要在下面的階梯上往上爬。
**原則：由簡到繁，被問題推著升級，不要預先套用重架構。**

| # | 方案 | 做法 | 一致性 | 事件語意 | 何時選它 |
|---|---|---|---|---|---|
| 0 | **同庫 View / JOIN** | 查詢直接讀寫入表 | 強一致 | — | 預設；量與形狀都還撐得住 |
| 1 | **Materialized View / 彙總表** | DB 定時或觸發刷新 | 秒~分級延遲 | — | 重彙總報表慢，但形狀還算接近 |
| 2 | **讀取副本（Read Replica）** | DB 原生複寫，同 schema | 毫秒~秒級延遲 | — | 問題只是**讀壓力**，不是形狀 |
| 3 | **同交易雙寫（同庫）** | 寫聚合時，同一個 DB 交易內順手更新讀表 | 強一致 | — | 讀表形狀不同但簡單、同一個庫；**跨庫雙寫是反模式**（部分失敗即分歧） |
| 4 | **領域事件 + Outbox → 投影** | 聚合發布業務事件，事件與業務資料**同交易**寫入 outbox 表，relay 送出後由 projector 建讀模型 | 最終一致 | **業務語意**（`FxPurchased`） | 讀模型形狀差很遠；事件本身另有價值（通知、審計、下游整合）；新建系統 |
| 5 | **CDC 抄寫**（Debezium/binlog） | 基礎設施層攔截**資料表列變更**，串流出去建讀模型 | 最終一致 | **資料列語意**（`FX_SUB_ACCOUNT` 某列 balance 變了） | **不能改或不想改寫入端程式**：舊核心、跨系統餵資料倉儲/客戶360 |
| 6 | **Event Sourcing + CQRS** | 事件即事實來源，狀態靠重播，讀模型全部投影 | 最終一致 | 業務語意 | 審計/重播是硬需求的少數領域；成本最高，**不要當預設** |

**CQRS vs. CDC 的關鍵差異不是技術，是「事件說什麼語言」**：

- 領域事件（方案 4）說**業務的語言**——「客戶用 32,500 台幣買了 1,000 美元」，
  下游不需要懂你的表結構，寫入表怎麼重構都不影響消費者。
- CDC（方案 5）說**資料表的語言**——「`FX_SUB_ACCOUNT` 第 N 列 `BALANCE` 從 x 變 y」，
  零侵入、不用改寫入端一行程式，但消費者從此**耦合你的 schema**，寫入表一改就波及下游。
  這其實就是本 repo 主題在同步機制上的重演：**方案 4 是 Domain Model 式的同步，方案 5 是 Data Model 式的同步**。

```mermaid
flowchart TD
    A[查詢撐不住了？] -->|只是讀壓力大| R[2 讀取副本]
    A -->|彙總報表慢| M[1 Materialized View]
    A -->|讀的形狀差很遠| B{寫入端能改程式嗎？}
    B -->|能，且事件有業務價值| O["4 領域事件 + Outbox"]
    B -->|不能改（舊核心/跨系統）| C[5 CDC 抄寫]
    O --> E{審計重播是硬需求？}
    E -->|是，且願付出成本| ES[6 Event Sourcing]
    E -->|否| STOP[到此為止]
```

**銀行實務的典型組合**：核心帳務同庫 View＋批次彙總表（方案 0/1）；
餵資料倉儲、客戶 360、監理報送用 CDC（方案 5）；
新一代微服務核心的跨服務整合用 Outbox 事件（方案 4）。
三者並存，各管一段——和本節開頭「寫入走 Domain Model、查詢走 Data Model」是同一個道理：
**按路徑選工具，而不是全系統押一種**。

最後一個銀行特有的約束：**客戶「讀自己剛寫的」必須強一致**
（轉完帳馬上看餘額，不能顯示舊值）——這條路徑要嘛走同庫查詢（方案 0），
要嘛在 UI 用命令的回傳值直接更新畫面；把它交給最終一致的讀模型（方案 4/5）是常見的翻車點。

---

## 七、延伸閱讀

- Eric Evans, *Domain-Driven Design*（藍皮書）— 聚合、Value Object、Repository 的出處
- Martin Fowler, *Patterns of Enterprise Application Architecture* — Transaction Script vs. Domain Model 兩個模式的原始定義
- Martin Fowler, [AnemicDomainModel](https://martinfowler.com/bliki/AnemicDomainModel.html) — 貧血模型為什麼是反模式
- Vaughn Vernon, *Implementing Domain-Driven Design*（紅皮書）— 聚合設計四原則（小聚合、以 ID 參照）
