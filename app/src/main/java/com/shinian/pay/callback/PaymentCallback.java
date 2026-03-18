package com.shinian.pay.callback;

/**
 * 支付回调监听接口
 */
public interface PaymentCallback {
    
    /**
     * 收到支付通知
     * @param type 支付类型 1=微信 2=支付宝
     * @param amount 支付金额
     */
    void onPaymentReceived(int type, double amount);
    
    /**
     * 心跳响应
     * @param success 是否成功
     * @param message 响应消息
     */
    void onHeartbeatResponse(boolean success, String message);
    
    /**
     * 日志更新
     * @param log 日志内容
     */
    void onLogUpdated(String log);
}
