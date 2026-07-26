package com.bank.domainmodel.web;

import com.bank.domainmodel.application.port.in.BuyForeignCurrencyUseCase;

import java.math.BigDecimal;

/**
 * 前端 Adapter（示意；實務上是 @RestController）。
 *
 * 相依方向是重點：這裡 import 的只有【輸入 Port 介面】——
 * 不認識 BuyForeignCurrencyService 實作、不認識聚合、更不認識 Repository。
 * 職責只有轉譯：HTTP 請求 → Command、Result → HTTP 回應（SRP）。
 */
public class FxPurchaseController {

    private final BuyForeignCurrencyUseCase buyForeignCurrency;   // 介面，不是實作

    public FxPurchaseController(BuyForeignCurrencyUseCase buyForeignCurrency) {
        this.buyForeignCurrency = buyForeignCurrency;
    }

    /** POST /fx/purchase */
    public BuyForeignCurrencyUseCase.Result purchase(String twdAccountNo, String fxAccountNo,
                                                     String twdAmount, String targetCurrency,
                                                     BigDecimal rate) {
        return buyForeignCurrency.execute(new BuyForeignCurrencyUseCase.Command(
                twdAccountNo, fxAccountNo, twdAmount, targetCurrency, rate));
    }
}
