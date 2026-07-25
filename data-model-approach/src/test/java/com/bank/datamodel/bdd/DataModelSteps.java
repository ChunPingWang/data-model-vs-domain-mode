package com.bank.datamodel.bdd;

import com.bank.datamodel.dao.InMemoryAccountDao;
import com.bank.datamodel.dao.InMemoryCreditCardDao;
import com.bank.datamodel.entity.AccountDO;
import com.bank.datamodel.entity.CreditCardDO;
import com.bank.datamodel.service.BankingService;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Data Model 側的 Step Definitions —— 驗收同一份 Gherkin 場景。
 *
 * 對照 DomainModelSteps 可看出兩個成本：
 *  1. Given 要手工組裝資料列（acctType="01"、status="A"、currency="TWD"…），
 *     測試被迫知道欄位碼這種「資料表方言」。
 *  2. 業務規則長在 BankingService 上，所有場景都得經過 Service + DAO 才測得到；
 *     Domain Model 側的規則長在聚合上，可以直接對物件測。
 */
public class DataModelSteps {

    private final InMemoryAccountDao accountDao = new InMemoryAccountDao();
    private final InMemoryCreditCardDao cardDao = new InMemoryCreditCardDao();
    private final BankingService service = new BankingService(accountDao, cardDao);

    private String twdAccountNo;
    private String fxAccountNo;
    private String cardNo;
    private RuntimeException lastError;

    // ---------- Given ----------

    @Given("客戶 {string} 的台幣帳戶 {string} 餘額為 {int} 元")
    public void 建立台幣帳戶(String customerId, String accountNo, int balance) {
        AccountDO row = new AccountDO();
        row.setAccountNo(accountNo);
        row.setCustomerId(customerId);
        row.setAcctType("01");
        row.setCurrency("TWD");
        row.setBalance(new BigDecimal(balance));
        row.setStatus("A");
        row.setOpenDate(LocalDate.of(2026, 1, 1));
        accountDao.insert(row);
        this.twdAccountNo = accountNo;
    }

    @Given("客戶 {string} 擁有外幣帳戶 {string}")
    public void 建立外幣帳戶(String customerId, String accountNo) {
        AccountDO row = new AccountDO();
        row.setAccountNo(accountNo);
        row.setCustomerId(customerId);
        row.setAcctType("03");
        row.setCurrency("USD");
        row.setBalance(BigDecimal.ZERO);
        row.setStatus("A");
        row.setOpenDate(LocalDate.of(2026, 1, 1));
        accountDao.insert(row);
        this.fxAccountNo = accountNo;
    }

    @Given("該台幣帳戶已被凍結")
    public void 凍結台幣帳戶() {
        AccountDO row = accountDao.findByAccountNo(twdAccountNo);
        row.setStatus("F");
        accountDao.update(row);
    }

    @Given("客戶 {string} 持有信用卡 {string} 額度 {int} 元")
    public void 建立信用卡(String customerId, String cardNoValue, int limit) {
        CreditCardDO row = new CreditCardDO();
        row.setCardNo(cardNoValue);
        row.setCustomerId(customerId);
        row.setCardType("02");
        row.setCreditLimit(new BigDecimal(limit));
        row.setUsedAmt(BigDecimal.ZERO);
        row.setBillAmt(BigDecimal.ZERO);
        row.setStatus("A");
        row.setExpireDate(LocalDate.of(2030, 12, 31));
        cardDao.insert(row);
        this.cardNo = cardNoValue;
    }

    @Given("已刷卡消費 {int} 元")
    public void 已刷卡消費(int amount) {
        service.chargeCreditCard(cardNo, new BigDecimal(amount));
    }

    @Given("該卡已掛失")
    public void 掛失() {
        CreditCardDO row = cardDao.findByCardNo(cardNo);
        row.setStatus("B");
        cardDao.update(row);
    }

    // ---------- When ----------

    @When("以匯率 {bigdecimal} 用台幣 {int} 元結購美元")
    public void 結購美元(BigDecimal rate, int twdAmount) {
        try {
            service.buyForeignCurrency(twdAccountNo, fxAccountNo, new BigDecimal(twdAmount), rate);
        } catch (RuntimeException e) {
            lastError = e;
        }
    }

    @When("存入台幣 {int} 元")
    public void 存入台幣(int amount) {
        service.deposit(twdAccountNo, "TWD", new BigDecimal(amount));
    }

    @When("提領台幣 {int} 元")
    public void 提領台幣(int amount) {
        try {
            service.withdraw(twdAccountNo, new BigDecimal(amount));
        } catch (RuntimeException e) {
            lastError = e;
        }
    }

    @When("刷卡消費 {int} 元")
    public void 刷卡消費(int amount) {
        try {
            service.chargeCreditCard(cardNo, new BigDecimal(amount));
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
        assertEquals(0, accountDao.findByAccountNo(twdAccountNo).getBalance()
                .compareTo(new BigDecimal(expected)));
    }

    @Then("外幣帳戶的美元子帳餘額應為 {int} 美元")
    public void 驗證美元子帳餘額(int expected) {
        assertEquals(0, accountDao.findByAccountNo(fxAccountNo).getBalance()
                .compareTo(new BigDecimal(expected)));
    }

    @Then("可用額度應為 {int} 元")
    public void 驗證可用額度(int expected) {
        CreditCardDO row = cardDao.findByCardNo(cardNo);
        BigDecimal available = row.getCreditLimit().subtract(row.getUsedAmt());
        assertEquals(0, available.compareTo(new BigDecimal(expected)));
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
}
