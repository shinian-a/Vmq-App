package com.shinian.pay.util;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Uri 转文件路径工具类
 *
 * <p>通过 BitmapUtil 读取图片并保存到临时文件的方式获取路径</p>
 * <p>相比直接解析 Uri 更加稳定可靠，适配所有 Android 版本</p>
 *
 * @see BitmapUtil
 */
public class UriUtil {
    /**
     * 根据图片的 Uri 获取图片的绝对路径
     * 
     * <p>实现原理：使用 BitmapUtil 读取图片并保存到应用缓存目录，返回文件路径</p>
     * <p>优点：适配所有 Android 版本，无需处理复杂的 Uri 格式和权限问题</p>
     * <p>注意：返回的是缓存文件路径，调用者负责在适当时机清理缓存</p>
     *
     * @param context 上下文
     * @param uri 图片 Uri
     * @return 如果 Uri 对应的图片存在，那么返回该图片的绝对路径，否则返回 null
     */
    public static String getRealPathFromUri(Context context, Uri uri) {
        if (context == null || uri == null) {
            return null;
        }
        
        try {
            // 使用 BitmapUtil 读取图片（限制最大尺寸避免 OOM）
            Bitmap bitmap = BitmapUtil.decodeUri(context, uri, 2048, 2048);
            if (bitmap == null) {
                Log.e("UriUtil", "Failed to decode bitmap from uri: " + uri);
                return null;
            }
            
            // 创建临时文件
            File cacheDir = context.getCacheDir();
            String fileName = "IMG_" + System.currentTimeMillis() + ".jpg";
            File imageFile = new File(cacheDir, fileName);
            
            // 将 Bitmap 写入文件
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(imageFile);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                fos.flush();
                return imageFile.getAbsolutePath();
            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException e) {
                        Log.e("UriUtil", "Failed to close file output stream", e);
                    }
                }
                // 回收 Bitmap 内存
                if (!bitmap.isRecycled()) {
                    bitmap.recycle();
                }
            }
        } catch (SecurityException e) {
            Log.e("UriUtil", "Permission denied when accessing uri: " + uri, e);
            return null;
        } catch (OutOfMemoryError e) {
            Log.e("UriUtil", "Out of memory when decoding bitmap", e);
            return null;
        } catch (Exception e) {
            Log.e("UriUtil", "Unexpected error when processing uri: " + uri, e);
            return null;
        }
    }

    // ==========================================================
    // 以下为保留的旧实现方法，已不再使用
    // 仅作为历史代码参考，建议直接使用 BitmapUtil
    // ==========================================================
    /**
     * 【已废弃】适配 api19 以上的旧实现
     * @deprecated 请使用新的 BitmapUtil 方案
     */
    @Deprecated
    private static String getRealPathFromUri_AboveApi19(Context context, Uri uri) {
        String filePath = null;
        Cursor cursor = null;
            
        try {
            String wholeID = DocumentsContract.getDocumentId(uri);
            if (wholeID == null || wholeID.isEmpty()) {
                return null;
            }
    
            // 使用':'分割
            String[] ids = wholeID.split(":");
            if (ids == null || ids.length == 0) {
                return null;
            }
                
            String id = ids.length > 1 ? ids[1] : ids[0];
            if (id == null || id.isEmpty()) {
                return null;
            }
    
            String[] projection = {MediaStore.Images.Media.DATA};
            String selection = MediaStore.Images.Media._ID + "=?";
            String[] selectionArgs = {id};
    
            cursor = context.getContentResolver().query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection, selection, selectionArgs, null);
                
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndex(projection[0]);
                if (columnIndex >= 0) {
                    filePath = cursor.getString(columnIndex);
                }
            }
        } catch (SecurityException e) {
            // 权限不足
            return null;
        } catch (Exception e) {
            // 其他异常
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
            
        return filePath;
    }

    /**
     * 【已废弃】适配 api11-api18 的旧实现
     * @deprecated 请使用新的 BitmapUtil 方案
     */
    @Deprecated
    private static String getRealPathFromUri_Api11To18(Context context, Uri uri) {
        String filePath = null;
        Cursor cursor = null;
            
        try {
            String[] projection = {MediaStore.Images.Media.DATA};
            CursorLoader loader = new CursorLoader(context, uri, projection, null, null, null);
            cursor = loader.loadInBackground();
    
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndex(projection[0]);
                if (columnIndex >= 0) {
                    filePath = cursor.getString(columnIndex);
                }
            }
        } catch (SecurityException e) {
            // 权限不足
            return null;
        } catch (Exception e) {
            // 其他异常
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
            
        return filePath;
    }

    /**
     * 【已废弃】适配 api11 以下的旧实现
     * @deprecated 请使用新的 BitmapUtil 方案
     */
    @Deprecated
    private static String getRealPathFromUri_BelowApi11(Context context, Uri uri) {
        String filePath = null;
        Cursor cursor = null;
            
        try {
            String[] projection = {MediaStore.Images.Media.DATA};
            cursor = context.getContentResolver().query(uri, projection, null, null, null);
                
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndex(projection[0]);
                if (columnIndex >= 0) {
                    filePath = cursor.getString(columnIndex);
                }
            }
        } catch (SecurityException e) {
            // 权限不足
            return null;
        } catch (Exception e) {
            // 其他异常
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
            
        return filePath;
    }
}
