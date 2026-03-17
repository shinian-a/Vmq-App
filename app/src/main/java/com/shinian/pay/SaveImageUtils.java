package com.shinian.pay;
import android.content.Context;
import android.graphics.Bitmap;
import java.io.File;
import android.os.Environment;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import android.provider.MediaStore;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;
import android.os.Build;
import android.content.ContentValues;
import java.io.OutputStream;


/**
 *  本地图片保存类
 */


public class SaveImageUtils {
    //仅适用于10.0以下
    public static void saveImageToGallery(Context context, Bitmap bmp) {
        // 首先保存图片
        File appDir = new File(Environment.getExternalStorageDirectory(), "Pictures");
        if (!appDir.exists()) {
            appDir.mkdir();
        }
        String fileName = System.currentTimeMillis() + ".jpg";
        File file = new File(appDir, fileName);
        try {
            FileOutputStream fos = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 其次把文件插入到系统图库
        try {
            MediaStore.Images.Media.insertImage(context.getContentResolver(),
                                                file.getAbsolutePath(), fileName, null);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        // 最后通知图库更新
        context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + file.getPath())));

    }
    public static void saveImageToGallerys(Context context, Bitmap bmp) {
        if (bmp == null){
            Toast.makeText(context, "保存出错了...", Toast.LENGTH_SHORT).show();
            return;
        }
        // 首先保存图片
        //File appDir = new File(BaseApplication.app.getTmpDir(), "ywq");
        File appDir = new File(Environment.getExternalStorageDirectory(), "Boohee");
        if (!appDir.exists()) {
            appDir.mkdir();
        }
        String fileName = System.currentTimeMillis() + ".jpg";
        File file = new File(appDir, fileName);
        try {
            FileOutputStream fos = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
        } catch (FileNotFoundException e) {
            Toast.makeText(context, "文件未发现...", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        } catch (IOException e) {
            Toast.makeText(context, "保存出错了...", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }catch (Exception e){
            Toast.makeText(context, "保存出错了...", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }

        // 最后通知图库更新
        try {
            MediaStore.Images.Media.insertImage(context.getContentResolver(), file.getAbsolutePath(), fileName, null);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri uri = Uri.fromFile(file);
        intent.setData(uri);
        context.sendBroadcast(intent);
        Toast.makeText(context, "保存出错了...", Toast.LENGTH_SHORT).show();
    }
    
    
    
    /**
     * 适用Android10及以上
     * 保存文件到公共目录
     * @param context 上下文
     * @param fileName 文件名
     * @param bitmap 文件
     * @return 路径，为空时表示保存失败
     */
    public static String fileSaveToPublic(Context context, String fileName, Bitmap bitmap) {
        String path = null;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            //Android 10以下版本
            FileOutputStream fos = null;
			//设置路径 Pictures/
			File folder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            try {
                //设置路径 Pictures/
                //File folder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                //判断目录是否存在
                //目录不存在时自动创建
                if (folder.exists() || folder.mkdir()) {
                    File file = new File(folder, fileName);
                    fos = new FileOutputStream(file);
                    //写入文件
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                    fos.flush();
                    path = file.getAbsolutePath();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
			//创建文件路径
			File file = new File(folder, fileName);
			// 其次把文件插入到系统图库
			/*try {
				MediaStore.Images.Media.insertImage(context.getContentResolver(),file.getAbsolutePath(), fileName, null);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}*/
			// 最后通知图库更新
			context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + file.getPath())));
			
			
			
        } else {
            //Android 10及以上版本

            //设置路径 Pictures/
            String folder = Environment.DIRECTORY_PICTURES;
            //设置保存参数到ContentValues中
            ContentValues values = new ContentValues();
            //设置图片名称
            values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
            //设置图片格式
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            //设置图片路径
            values.put(MediaStore.Images.Media.RELATIVE_PATH, folder);
            //执行insert操作，向系统文件夹中添加文件
            //EXTERNAL_CONTENT_URI代表外部存储器，该值不变
            Uri uri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            OutputStream os = null;
            try {
                if (uri != null) {
                    //若生成了uri，则表示该文件添加成功
                    //使用流将内容写入该uri中即可
                    os = context.getContentResolver().openOutputStream(uri);
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
                    os.flush();
                    path = uri.getPath();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (os != null) {
                    try {
                        os.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return path;
    }
    
}
