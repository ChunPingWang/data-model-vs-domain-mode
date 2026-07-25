package com.bank.datamodel.demo;

import com.bank.datamodel.dao.InMemoryAccountDao;
import com.bank.datamodel.dao.InMemoryCreditCardDao;
import com.bank.datamodel.entity.AccountDO;
import com.bank.datamodel.service.BankingService;
import com.bank.datamodel.web.AccountController;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Data Model 途徑端到端驗證：
 * Controller → BankingService（Transaction Script）→ DAO（一表一 DAO）。
 *
 * 執行方式（在 data-model-approach/ 下）：
 *   javac -d out $(find src -name '*.java')
 *   java -cp out com.bank.datamodel.demo.DataModelDemo
 */
public class DataModelDemo {

    public static void main(String[] args) {
        InMemoryAccountDao accountDao = new InMemoryAccountDao();
        InMemoryCreditCardDao cardDao = new InMemoryCreditCardDao();
        BankingService service = new BankingService(accountDao, cardDao);
        AccountController controller = new AccountController(accountDao, service);

        accountDao.insert(newAccount("0011223344556", "C000001", "01", "TWD", "100000"));
        accountDao.insert(newAccount("0099887766554", "C000001", "03", "USD", "0"));

        System.out.println("=== [正常流程] 台幣 32,500 結購美元（匯率 32.5）===");
        controller.buyForeignCurrency("0011223344556", "0099887766554",
                new BigDecimal("32500"), new BigDecimal("32.5"));
        AccountDO twd = controller.getAccount("0011223344556");
        AccountDO usd = controller.getAccount("0099887766554");
        System.out.println("前端拿到的回應（DO 原樣序列化，碼值需前端自行翻譯）：");
        System.out.printf("  {accountNo:%s, acctType:%s, currency:%s, balance:%s, status:%s}%n",
                twd.getAccountNo(), twd.getAcctType(), twd.getCurrency(), twd.getBalance(), twd.getStatus());
        System.out.printf("  {accountNo:%s, acctType:%s, currency:%s, balance:%s, status:%s}%n",
                usd.getAccountNo(), usd.getAcctType(), usd.getCurrency(), usd.getBalance(), usd.getStatus());

        System.out.println();
        System.out.println("=== [防線示範] Service 有檢查的路徑，擋得住 ===");
        try {
            service.withdraw("0011223344556", new BigDecimal("999999"));
        } catch (IllegalStateException e) {
            System.out.println("  提款超過餘額 → 被 Service 擋下：" + e.getMessage());
        }

        System.out.println();
        System.out.println("=== [資料洞示範] 繞過 Service 直接操作 DO，什麼都擋不住 ===");
        AccountDO hacked = accountDao.findByAccountNo("0011223344556");
        hacked.setBalance(new BigDecimal("-50000"));            // 負餘額：直接寫入成功
        accountDao.update(hacked);
        System.out.println("  setBalance(-50000) 後餘額 = "
                + accountDao.findByAccountNo("0011223344556").getBalance() + "  ← 不變量已被破壞");

        AccountDO fx = accountDao.findByAccountNo("0099887766554");
        fx.setBalance(fx.getBalance().add(new BigDecimal("1000")));  // 意圖是加台幣 1000
        accountDao.update(fx);
        System.out.println("  台幣 1000 誤加進 USD 帳戶餘額 = "
                + accountDao.findByAccountNo("0099887766554").getBalance()
                + "  ← 幣別錯帳，編譯期與執行期都無感");
    }

    private static AccountDO newAccount(String no, String custId, String type,
                                        String currency, String balance) {
        AccountDO account = new AccountDO();
        account.setAccountNo(no);
        account.setCustomerId(custId);
        account.setAcctType(type);
        account.setCurrency(currency);
        account.setBalance(new BigDecimal(balance));
        account.setStatus("A");
        account.setOpenDate(LocalDate.of(2026, 1, 1));
        return account;
    }
}
