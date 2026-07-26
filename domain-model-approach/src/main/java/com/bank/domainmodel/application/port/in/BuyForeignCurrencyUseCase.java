package com.bank.domainmodel.application.port.in;

import java.math.BigDecimal;

/**
 * 輸入 Port（Inbound Port）：前端 Adapter 唯一能相依的東西。
 *
 * DIP：Web Controller 依賴這個「抽象」，不依賴 BuyForeignCurrencyService 實作；
 * ISP：一個 Use Case 一個介面，前端不會被迫依賴用不到的方法。
 */
public interface BuyForeignCurrencyUseCase {

    Result execute(Command command);

    /** 前端送進來的命令：在系統邊界立刻轉成領域型別，錯誤最早爆開。 */
    record Command(String twdAccountNo, String fxAccountNo,
                   String twdAmount, String targetCurrency, BigDecimal rate) {}

    /** 回給前端的 DTO：語意化欄位，與聚合內部結構、資料表結構都脫鉤。 */
    record Result(String debitedTwdAmount, String purchasedFxAmount,
                  String fxCurrency, String remainingTwdBalance) {}
}
