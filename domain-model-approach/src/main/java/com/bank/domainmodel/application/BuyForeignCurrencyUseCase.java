package com.bank.domainmodel.application;

import com.bank.domainmodel.account.AccountNumber;
import com.bank.domainmodel.account.ForeignCurrencyAccount;
import com.bank.domainmodel.account.TwdAccount;
import com.bank.domainmodel.repository.ForeignCurrencyAccountRepository;
import com.bank.domainmodel.repository.TwdAccountRepository;
import com.bank.domainmodel.service.CurrencyExchangeService;
import com.bank.domainmodel.shared.ExchangeRate;
import com.bank.domainmodel.shared.Money;

import java.math.BigDecimal;
import java.util.Currency;

/**
 * Application Service（Use Case）：Domain Model 途徑對前端的唯一窗口。
 *
 * 職責只有編排：載入聚合 → 呼叫領域行為 → 存回聚合 → 轉成 DTO 回前端。
 * 它不含任何業務規則（規則在聚合與 Domain Service 裡），
 * 也不讓聚合外洩到 API 契約 —— 前端拿到的是 Result DTO，不是領域物件。
 */
public class BuyForeignCurrencyUseCase {

    /** 前端送進來的命令：在系統邊界立刻轉成領域型別，錯誤最早爆開。 */
    public record Command(String twdAccountNo, String fxAccountNo,
                          String twdAmount, String targetCurrency, BigDecimal rate) {}

    /** 回給前端的 DTO：語意化欄位，與聚合內部結構、資料表結構都脫鉤。 */
    public record Result(String debitedTwdAmount, String purchasedFxAmount,
                         String fxCurrency, String remainingTwdBalance) {}

    private final TwdAccountRepository twdAccountRepository;
    private final ForeignCurrencyAccountRepository fxAccountRepository;
    private final CurrencyExchangeService exchangeService;

    public BuyForeignCurrencyUseCase(TwdAccountRepository twdAccountRepository,
                                     ForeignCurrencyAccountRepository fxAccountRepository,
                                     CurrencyExchangeService exchangeService) {
        this.twdAccountRepository = twdAccountRepository;
        this.fxAccountRepository = fxAccountRepository;
        this.exchangeService = exchangeService;
    }

    public Result execute(Command command) {
        TwdAccount from = twdAccountRepository
                .findByAccountNumber(new AccountNumber(command.twdAccountNo()))
                .orElseThrow(() -> new IllegalArgumentException("台幣帳戶不存在"));
        ForeignCurrencyAccount to = fxAccountRepository
                .findByAccountNumber(new AccountNumber(command.fxAccountNo()))
                .orElseThrow(() -> new IllegalArgumentException("外幣帳戶不存在"));

        Money twdAmount = Money.twd(command.twdAmount());
        ExchangeRate rate = new ExchangeRate(
                Money.TWD, Currency.getInstance(command.targetCurrency()), command.rate());

        Money purchased = exchangeService.buyForeignCurrency(from, to, twdAmount, rate);

        twdAccountRepository.save(from);   // 傳整個聚合，不是傳一列資料
        fxAccountRepository.save(to);

        return new Result(twdAmount.toString(), purchased.toString(),
                purchased.currency().getCurrencyCode(), from.balance().toString());
    }
}
