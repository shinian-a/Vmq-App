package com.shinian.pay.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

/**
 * 虚拟 ContentProvider
 * 用于配合 SyncAdapter 工作，不需要实际的数据存储功能
 */
public class SyncProvider extends ContentProvider {

    private static final String TAG = "SyncProvider";

    /**
     * 创建时调用
     */
    @Override
    public boolean onCreate() {
        Log.d(TAG, "SyncProvider 已创建");
        // 返回 true 表示 Provider 创建成功
        return true;
    }

    /**
     * 查询数据（本应用不需要）
     */
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, 
                       String[] selectionArgs, String sortOrder) {
        // 本应用不需要实际查询数据
        return null;
    }

    /**
     * 获取 MIME 类型（本应用不需要）
     */
    @Override
    public String getType(Uri uri) {
        // 本应用不需要
        return null;
    }

    /**
     * 插入数据（本应用不需要）
     */
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // 本应用不需要
        return null;
    }

    /**
     * 删除数据（本应用不需要）
     */
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // 本应用不需要
        return 0;
    }

    /**
     * 更新数据（本应用不需要）
     */
    @Override
    public int update(Uri uri, ContentValues values, String selection, 
                     String[] selectionArgs) {
        // 本应用不需要
        return 0;
    }
}
