package com.xiantao.exception;

/**
 * 库存不足异常
 * 用于区分"DB 库存不足"与其他处理失败：DB 库存不足说明 Redis 与 DB 库存不一致，
 * 死信补偿链路中不应回补 Redis 库存（回补会放大超卖），需告警人工核查。
 */
public class StockEmptyException extends RuntimeException {

    public StockEmptyException(String message) {
        super(message);
    }
}
