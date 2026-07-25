package com.bank.domainmodel.demo;

import com.bank.domainmodel.account.AccountNumber;
import com.bank.domainmodel.account.ForeignCurrencyAccount;
import com.bank.domainmodel.account.InsufficientBalanceException;
import com.bank.domainmodel.account.TwdAccount;
import com.bank.domainmodel.application.BuyForeignCurrencyUseCase;
import com.bank.domainmodel.card.CardNumber;
import com.bank.domainmodel.card.CreditCard;
import com.bank.domainmodel.card.CreditLimitExceededException;
import com.bank.domainmodel.customer.CustomerId;
import com.bank.domainmodel.infrastructure.persistence.InMemoryForeignCurrencyAccountRepository;
import com.bank.domainmodel.infrastructure.persistence.InMemoryTwdAccountRepository;
import com.bank.domainmodel.service.CurrencyExchangeService;
import com.bank.domainmodel.shared.CurrencyMismatchException;
import com.bank.domainmodel.shared.Money;

import java.math.BigDecimal;
import java.time.YearMonth;

/**
 * Domain Model 途徑端到端驗證：
 * UseCase（Application Service）→ 聚合/Domain Service → Repository（一聚合一 Repository）。
 *
 * 執行方式（在 domain-model-approach/ 下）：
 *   javac -d out $(find src -name '*.java')
 *   java -cp out com.bank.domainmodel.demo.DomainModelDemo
 */
public class DomainModelDemo {

    public static void main(String[] args) {
        InMemoryTwdAccountRepository twdRepo = new InMemoryTwdAccountRepository();
        InMemoryForeignCurrencyAccountRepository fxRepo = new InMemoryForeignCurrencyAccountRepository();
        BuyForeignCurrencyUseCase useCase = new BuyForeignCurrencyUseCase(
                twdRepo, fxRepo, new CurrencyExchangeService());

        CustomerId customer = new CustomerId("C000001");
        TwdAccount twdAccount = new TwdAccount(new AccountNumber("0011223344556"), customer);
        twdAccount.deposit(Money.twd("100000"));
        twdRepo.save(twdAccount);
        fxRepo.save(new ForeignCurrencyAccount(new AccountNumber("0099887766554"), customer));

        System.out.println("=== [正常流程] 台幣 32,500 結購美元（匯率 32.5）===");
        BuyForeignCurrencyUseCase.Result result = useCase.execute(
                new BuyForeignCurrencyUseCase.Command(
                        "0011223344556", "0099887766554", "32500", "USD", new BigDecimal("32.5")));
        System.out.println("前端拿到的回應（語意化 DTO，與資料表結構脫鉤）：");
        System.out.println("  " + result);
        System.out.println("Repository 落地結果（一個聚合寫入兩張表）：");
        fxRepo.dumpSubAccountTable().forEach((accountNo, rows) -> rows.forEach(row ->
                System.out.printf("  FX_SUB_ACCOUNT: {accountNo:%s, currency:%s, balance:%s}%n",
                        row.accountNo, row.currency, row.balance)));

        System.out.println();
        System.out.println("=== [不變量驗證] 非法操作在模型層就被擋下 ===");

        TwdAccount reloaded = twdRepo.findByAccountNumber(new AccountNumber("0011223344556")).orElseThrow();
        try {
            reloaded.withdraw(Money.twd("999999"));
        } catch (InsufficientBalanceException e) {
            System.out.println("  1. 提款超過餘額 → " + e.getMessage());
        }

        try {
            Money.twd("1000").add(Money.of(new BigDecimal("100"), Money.USD));
        } catch (CurrencyMismatchException e) {
            System.out.println("  2. 台幣 + 美元 → " + e.getMessage());
        }

        ForeignCurrencyAccount fxAccount =
                fxRepo.findByAccountNumber(new AccountNumber("0099887766554")).orElseThrow();
        try {
            fxAccount.deposit(Money.twd("1000"));
        } catch (CurrencyMismatchException e) {
            System.out.println("  3. 台幣存入外幣帳戶 → " + e.getMessage());
        }

        CreditCard card = new CreditCard(new CardNumber("4111111111111111"), customer,
                Money.twd("50000"), YearMonth.of(2030, 12));
        card.authorize(Money.twd("48000"), YearMonth.of(2026, 7));
        try {
            card.authorize(Money.twd("5000"), YearMonth.of(2026, 7));
        } catch (CreditLimitExceededException e) {
            System.out.println("  4. 刷卡超過額度 → " + e.getMessage());
        }

        System.out.println();
        System.out.println("  （對照組：這一側不存在 setBalance()，「負餘額」寫不出來，編譯就失敗）");
    }
}
