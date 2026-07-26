package com.bank.domainmodel.application;

import com.bank.domainmodel.account.AccountNumber;
import com.bank.domainmodel.account.ForeignCurrencyAccount;
import com.bank.domainmodel.account.TwdAccount;
import com.bank.domainmodel.application.port.in.BuyForeignCurrencyUseCase;
import com.bank.domainmodel.repository.ForeignCurrencyAccountRepository;
import com.bank.domainmodel.repository.TwdAccountRepository;
import com.bank.domainmodel.service.CurrencyExchangeService;
import com.bank.domainmodel.shared.ExchangeRate;
import com.bank.domainmodel.shared.Money;

import java.util.Currency;

/**
 * Application Service：輸入 Port 的實作。
 *
 * 相依方向（全部指向抽象或核心，不指向任何 Adapter）：
 *   實作 BuyForeignCurrencyUseCase（輸入 Port）←—— Web Adapter 依賴該介面
 *   依賴 TwdAccountRepository / ForeignCurrencyAccountRepository（輸出 Port 介面）
 *        ←—— 由 infrastructure 的實作在組裝時注入（DIP）
 *
 * 職責只有編排：載入聚合 → 呼叫領域行為 → 存回聚合 → 轉 DTO（SRP）。
 * 規則在聚合與 Domain Service 裡，這裡一條都沒有。
 */
public class BuyForeignCurrencyService implements BuyForeignCurrencyUseCase {

    private final TwdAccountRepository twdAccountRepository;
    private final ForeignCurrencyAccountRepository fxAccountRepository;
    private final CurrencyExchangeService exchangeService;

    public BuyForeignCurrencyService(TwdAccountRepository twdAccountRepository,
                                     ForeignCurrencyAccountRepository fxAccountRepository,
                                     CurrencyExchangeService exchangeService) {
        this.twdAccountRepository = twdAccountRepository;
        this.fxAccountRepository = fxAccountRepository;
        this.exchangeService = exchangeService;
    }

    @Override
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
