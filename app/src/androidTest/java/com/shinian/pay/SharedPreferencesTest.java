package com.shinian.pay;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * androidTest 示例 - 测试 SharedPreferences
 * 
 * 这个测试需要在 Android 设备或模拟器上运行
 * 依赖于真实的 Android Context 和 SharedPreferences
 */
@RunWith(AndroidJUnit4.class)
public class SharedPreferencesTest {

    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "test_prefs";
    private static final String KEY_HOST = "host";
    private static final String KEY_KEY = "key";

    @Before
    public void setUp() {
        // 获取 Android Context
        Context context = InstrumentationRegistry.getInstrumentation().getContext();
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        
        // 清理测试数据
        sharedPreferences.edit().clear().commit();
    }

    @After
    public void tearDown() {
        // 清理测试数据
        sharedPreferences.edit().clear().commit();
    }

    /**
     * 测试保存和读取字符串
     */
    @Test
    public void testSaveAndReadString() {
        // 保存数据
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_HOST, "example.com");
        editor.putString(KEY_KEY, "test_key_123");
        editor.commit();

        // 读取数据
        String host = sharedPreferences.getString(KEY_HOST, "");
        String key = sharedPreferences.getString(KEY_KEY, "");

        // 验证
        assertEquals("example.com", host);
        assertEquals("test_key_123", key);
        
        System.out.println("✅ 测试: 保存和读取字符串");
        System.out.println("   Host: " + host);
        System.out.println("   Key: " + key);
    }

    /**
     * 测试默认值
     */
    @Test
    public void testDefaultValue() {
        // 读取不存在的数据，应该返回默认值
        String host = sharedPreferences.getString("non_existent_key", "default_value");
        
        assertEquals("default_value", host);
        
        System.out.println("✅ 测试: 默认值");
        System.out.println("   默认值: " + host);
    }

    /**
     * 测试删除数据
     */
    @Test
    public void testRemoveData() {
        // 先保存数据
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_HOST, "example.com");
        editor.commit();
        
        // 验证已保存
        assertEquals("example.com", sharedPreferences.getString(KEY_HOST, ""));
        
        // 删除数据
        editor.remove(KEY_HOST);
        editor.commit();
        
        // 验证已删除（应返回空字符串）
        assertEquals("", sharedPreferences.getString(KEY_HOST, ""));
        
        System.out.println("✅ 测试: 删除数据");
        System.out.println("   删除后值: " + sharedPreferences.getString(KEY_HOST, ""));
    }

    /**
     * 测试清空所有数据
     */
    @Test
    public void testClearAllData() {
        // 保存多个数据
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_HOST, "example.com");
        editor.putString(KEY_KEY, "test_key");
        editor.commit();
        
        // 清空所有数据
        editor.clear();
        editor.commit();
        
        // 验证所有数据都被清空
        assertEquals("", sharedPreferences.getString(KEY_HOST, ""));
        assertEquals("", sharedPreferences.getString(KEY_KEY, ""));
        
        System.out.println("✅ 测试: 清空所有数据");
    }
}
