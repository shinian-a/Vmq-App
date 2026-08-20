package com.shinian.pay.service;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * PayNotificationListenerService 单元测试
 */
public class PayNotificationListenerServiceTest {

    /**
     * 测试从正常收款通知中提取金额
     */
    @Test
    public void testGetMoney_NormalWechatPayment() {
        String content = "微信支付收款1.00元";
        String result = PayNotificationListenerService.getMoney(content);
        System.out.println("✅ 测试: 正常微信收款");
        System.out.println("   输入: " + content);
        System.out.println("   提取金额: " + result);
        assertEquals("1.00", result);
    }

    /**
     * 测试从支付宝收款通知中提取金额
     */
    @Test
    public void testGetMoney_NormalAlipayPayment() {
        String content = "支付宝成功收款100.50元";
        String result = PayNotificationListenerService.getMoney(content);
        System.out.println("✅ 测试: 正常支付宝收款");
        System.out.println("   输入: " + content);
        System.out.println("   提取金额: " + result);
        assertEquals("100.50", result);
    }

    /**
     * 测试从小数金额中提取
     */
    @Test
    public void testGetMoney_DecimalAmount() {
        String content = "微信收款0.01元";
        String result = PayNotificationListenerService.getMoney(content);
        System.out.println("✅ 测试: 小数金额提取");
        System.out.println("   输入: " + content);
        System.out.println("   提取金额: " + result);
        assertEquals("0.01", result);
    }

    /**
     * 测试从大金额中提取
     */
    @Test
    public void testGetMoney_LargeAmount() {
        String content = "支付宝收款999999.99元";
        String result = PayNotificationListenerService.getMoney(content);
        System.out.println("✅ 测试: 大金额提取");
        System.out.println("   输入: " + content);
        System.out.println("   提取金额: " + result);
        assertEquals("999999.99", result);
    }

    /**
     * 测试没有"收款"关键字的情况
     */
    @Test
    public void testGetMoney_NoShoukuanKeyword() {
        String content = "微信支付1.00元";
        String result = PayNotificationListenerService.getMoney(content);
        System.out.println("✅ 测试: 无收款关键字");
        System.out.println("   输入: " + content);
        System.out.println("   提取结果: " + result + " (应为null)");
        assertNull(result);
    }

    /**
     * 测试空字符串输入
     */
    @Test
    public void testGetMoney_EmptyString() {
        String result = PayNotificationListenerService.getMoney("");
        System.out.println("✅ 测试: 空字符串输入");
        System.out.println("   提取结果: " + result + " (应为null)");
        assertNull(result);
    }

    /**
     * 测试null输入
     */
    @Test
    public void testGetMoney_NullInput() {
        String result = PayNotificationListenerService.getMoney(null);
        System.out.println("✅ 测试: null输入");
        System.out.println("   提取结果: " + result + " (应为null)");
        assertNull(result);
    }

    /**
     * 测试多个数字，应该返回第一个有效金额
     */
    @Test
    public void testGetMoney_MultipleNumbers() {
        String content = "订单号123456收款50.00元，余额1000.00元";
        String result = PayNotificationListenerService.getMoney(content);
        System.out.println("✅ 测试: 多个数字提取");
        System.out.println("   输入: " + content);
        System.out.println("   提取金额: " + result + " (应返回'收款'后的第一个有效金额)");
        assertEquals("50.00", result);
    }

    /**
     * 测试超出合理范围的金额（过大）
     */
    @Test
    public void testGetMoney_AmountTooLarge() {
        String content = "收款1000000.00元";
        String result = PayNotificationListenerService.getMoney(content);
        System.out.println("✅ 测试: 金额过大（超出范围）");
        System.out.println("   输入: " + content);
        System.out.println("   提取结果: " + result + " (应被过滤，返回null)");
        assertNull(result);
    }

    /**
     * 测试超出合理范围的金额（过小）
     */
    @Test
    public void testGetMoney_AmountTooSmall() {
        String content = "收款0.001元";
        String result = PayNotificationListenerService.getMoney(content);
        System.out.println("✅ 测试: 金额过小（超出范围）");
        System.out.println("   输入: " + content);
        System.out.println("   提取结果: " + result + " (应被过滤，返回null)");
        assertNull(result);
    }

    /**
     * 测试整数金额
     */
    @Test
    public void testGetMoney_IntegerAmount() {
        String content = "微信收款100元";
        String result = PayNotificationListenerService.getMoney(content);
        System.out.println("✅ 测试: 整数金额提取");
        System.out.println("   输入: " + content);
        System.out.println("   提取金额: " + result);
        assertEquals("100", result);
    }

    /**
     * 测试包含特殊字符的文本
     */
    @Test
    public void testGetMoney_WithSpecialCharacters() {
        String content = "[微信]收款￥88.88元[成功]";
        String result = PayNotificationListenerService.getMoney(content);
        System.out.println("✅ 测试: 包含特殊字符的文本");
        System.out.println("   输入: " + content);
        System.out.println("   提取金额: " + result);
        assertEquals("88.88", result);
    }

    /**
     * 测试企业微信收款
     */
    @Test
    public void testGetMoney_WechatWork() {
        String content = "企业微信收款200.00元";
        String result = PayNotificationListenerService.getMoney(content);
        System.out.println("✅ 测试: 企业微信收款");
        System.out.println("   输入: " + content);
        System.out.println("   提取金额: " + result);
        assertEquals("200.00", result);
    }

    /**
     * 测试边界值：最小有效金额
     */
    @Test
    public void testGetMoney_MinValidAmount() {
        String content = "收款0.01元";
        String result = PayNotificationListenerService.getMoney(content);
        System.out.println("✅ 测试: 最小有效金额（边界值）");
        System.out.println("   输入: " + content);
        System.out.println("   提取金额: " + result);
        assertEquals("0.01", result);
    }

    /**
     * 测试边界值：最大有效金额
     */
    @Test
    public void testGetMoney_MaxValidAmount() {
        String content = "收款999999.99元";
        String result = PayNotificationListenerService.getMoney(content);
        System.out.println("✅ 测试: 最大有效金额（边界值）");
        System.out.println("   输入: " + content);
        System.out.println("   提取金额: " + result);
        assertEquals("999999.99", result);
    }

    /**
     * 测试无效数字格式（以小数点开头）-暂时用不到不做单元测试标准
     */
    @Test
    public void testGetMoney_InvalidNumberFormat() {
        String content = "收款.50元";
        String result = PayNotificationListenerService.getMoney(content);
        System.out.println("✅ 测试: 无效数字格式（以小数点开头）-暂时用不到不做单元测试标准");
        System.out.println("   输入: " + content);
        System.out.println("   提取结果: " + result + " (.50不是有效格式，应返回null)");
        assertNull(result);
    }

    /**
     * 测试复杂的通知文本-暂时用不到不做单元测试标准
     */
    @Test
    public void testGetMoney_ComplexNotification() {
        String content = "【微信支付】微信支付收款凭证\n交易时间：2024-01-01 12:00:00\n收款金额：158.88元\n收款方：商户名称";
        String result = PayNotificationListenerService.getMoney(content);
        System.out.println("✅ 测试: 复杂的通知文本-暂时用不到不做单元测试标准");
        System.out.println("   输入: " + content.replace("\n", "\\n"));
        System.out.println("   提取金额: " + result + " (应提取158.88而非2024)");
        assertEquals("158.88", result);
    }
}
