package com.bank.domainmodel.bdd;

import com.bank.domainmodel.account.AccountNumber;
import com.bank.domainmodel.account.ForeignCurrencyAccount;
import com.bank.domainmodel.account.TwdAccount;
import com.bank.domainmodel.application.BuyForeignCurrencyUseCase;
import com.bank.domainmodel.card.CardNumber;
import com.bank.domainmodel.card.CreditCard;
import com.bank.domainmodel.customer.CustomerId;
import com.bank.domainmodel.infrastructure.persistence.InMemoryForeignCurrencyAccountRepository;
import com.bank.domainmodel.infrastructure.persistence.InMemoryTwdAccountRepository;
import com.bank.domainmodel.service.CurrencyExchangeService;
import com.bank.domainmodel.shared.Money;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.math.BigDecimal;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Domain Model 側的 Step Definitions。
 *
 * 對照 DataModelSteps 可看出：Given 直接 new 聚合、行為直接呼叫領域方法，
 * 不需要準備欄位碼（acctType="01"、status="A"）這類資料表細節。
 * 測試講的語言和 prod. code 講的語言是同一套。
 */
public class DomainModelSteps {

    private static final YearMonth TODAY = YearMonth.of(2026, 7);

    private final InMemoryTwdAccountRepository twdRepo = new InMemoryTwdAccountRepository();
    private final InMemoryForeignCurrencyAccountRepository fxRepo =
            new InMemoryForeignCurrencyAccountRepository();
    private final BuyForeignCurrencyUseCase buyFxUseCase =
            new BuyForeignCurrencyUseCase(twdRepo, fxRepo, new CurrencyExchangeService());

    private String twdAccountNo;
    private String fxAccountNo;
    private CreditCard card;
    private RuntimeException lastError;

    // ---------- Given ----------

    @Given("客戶 {string} 的台幣帳戶 {string} 餘額為 {int} 元")
    public void 建立台幣帳戶(String customerId, String accountNo, int balance) {
        TwdAccount account = new TwdAccount(new AccountNumber(accountNo), new CustomerId(customerId));
        if (balance > 0) {
            account.deposit(Money.twd(String.valueOf(balance)));
        }
        twdRepo.save(account);
        this.twdAccountNo = accountNo;
    }

    @Given("客戶 {string} 擁有外幣帳戶 {string}")
    public void 建立外幣帳戶(String customerId, String accountNo) {
        fxRepo.save(new ForeignCurrencyAccount(new AccountNumber(accountNo), new CustomerId(customerId)));
        this.fxAccountNo = accountNo;
    }

    @Given("該台幣帳戶已被凍結")
    public void 凍結台幣帳戶() {
        TwdAccount account = loadTwdAccount();
        account.freeze();
        twdRepo.save(account);
    }

    @Given("客戶 {string} 持有信用卡 {string} 額度 {int} 元")
    public void 建立信用卡(String customerId, String cardNo, int limit) {
        card = new CreditCard(new CardNumber(cardNo), new CustomerId(customerId),
                Money.twd(String.valueOf(limit)), YearMonth.of(2030, 12));
    }

    @Given("已刷卡消費 {int} 元")
    public void 已刷卡消費(int amount) {
        card.authorize(Money.twd(String.valueOf(amount)), TODAY);
    }

    @Given("該卡已掛失")
    public void 掛失() {
        card.reportLost();
    }

    // ---------- When ----------

    @When("以匯率 {bigdecimal} 用台幣 {int} 元結購美元")
    public void 結購美元(BigDecimal rate, int twdAmount) {
        try {
            buyFxUseCase.execute(new BuyForeignCurrencyUseCase.Command(
                    twdAccountNo, fxAccountNo, String.valueOf(twdAmount), "USD", rate));
        } catch (RuntimeException e) {
            lastError = e;
        }
    }

    @When("存入台幣 {int} 元")
    public void 存入台幣(int amount) {
        TwdAccount account = loadTwdAccount();
        account.deposit(Money.twd(String.valueOf(amount)));
        twdRepo.save(account);
    }

    @When("提領台幣 {int} 元")
    public void 提領台幣(int amount) {
        try {
            TwdAccount account = loadTwdAccount();
            account.withdraw(Money.twd(String.valueOf(amount)));
            twdRepo.save(account);
        } catch (RuntimeException e) {
            lastError = e;
        }
    }

    @When("刷卡消費 {int} 元")
    public void 刷卡消費(int amount) {
        try {
            card.authorize(Money.twd(String.valueOf(amount)), TODAY);
        } catch (RuntimeException e) {
            lastError = e;
        }
    }

    // ---------- Then ----------

    @Then("交易成功")
    public void 交易成功() {
        assertNull(lastError, "不應有任何錯誤");
    }

    @Then("授權成功")
    public void 授權成功() {
        assertNull(lastError, "不應有任何錯誤");
    }

    @Then("台幣帳戶餘額應為 {int} 元")
    public void 驗證台幣餘額(int expected) {
        assertEquals(Money.twd(String.valueOf(expected)), loadTwdAccount().balance());
    }

    @Then("外幣帳戶的美元子帳餘額應為 {int} 美元")
    public void 驗證美元子帳餘額(int expected) {
        ForeignCurrencyAccount account =
                fxRepo.findByAccountNumber(new AccountNumber(fxAccountNo)).orElseThrow();
        assertEquals(Money.of(new BigDecimal(expected), Money.USD), account.balanceOf(Money.USD));
    }

    @Then("可用額度應為 {int} 元")
    public void 驗證可用額度(int expected) {
        assertEquals(Money.twd(String.valueOf(expected)), card.availableCredit());
    }

    @Then("交易應被拒絕並提示 {string}")
    public void 交易應被拒絕(String keyword) {
        assertNotNull(lastError, "預期交易被拒絕，實際卻成功了");
        assertTrue(lastError.getMessage().contains(keyword),
                "錯誤訊息「" + lastError.getMessage() + "」未包含「" + keyword + "」");
    }

    @Then("授權應被拒絕並提示 {string}")
    public void 授權應被拒絕並提示(String keyword) {
        交易應被拒絕(keyword);
    }

    @Then("授權應被拒絕")
    public void 授權應被拒絕() {
        assertNotNull(lastError, "預期授權被拒絕，實際卻成功了");
    }

    private TwdAccount loadTwdAccount() {
        return twdRepo.findByAccountNumber(new AccountNumber(twdAccountNo)).orElseThrow();
    }
}
