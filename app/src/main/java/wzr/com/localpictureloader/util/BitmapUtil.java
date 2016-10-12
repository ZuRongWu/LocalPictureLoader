package wzr.com.localpictureloader.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.util.Log;

import java.io.File;

/**
 * bitmap工具类
 * Created by wuzr on 2016/9/12.
 */
public class BitmapUtil {
    private static final String TAG = "BitmapUtil";
    private static final int NO_LIMIT_W = -1;
    private static final int NO_LIMIT_H = -1;

    /**
     * 从sour指定的文件中加载图片
     * @param context 上下文
     * @param sour 目标文件
     * @param fitScreen true则图片宽高将适应屏幕宽高，在加载大图时可以节省内存
     * @return 从sour加载到的图片
     */
    public static Bitmap loadBitmapFromFile(Context context, File sour, boolean fitScreen) {
        int dirWidth, dirHeight;
        if (fitScreen) {
            DisplayMetrics dm = context.getResources().getDisplayMetrics();
            dirWidth = dm.widthPixels;
            dirHeight = dm.heightPixels;
        } else {
            dirWidth = NO_LIMIT_W;
            dirHeight = NO_LIMIT_H;
        }
        return FileUtil.readImageFromFile(sour,null,dirWidth,dirHeight);
    }

    /**
     * 把bitmap保存到file指定的文件中
     * @param file 保存bitmap的文件
     * @param bitmap 将要保存的图片
     * @return 保存bitmap的文件
     */
    public static File saveBitmap(File file, Bitmap bitmap) {
        if (file == null || !file.exists()) {
            Log.w(TAG, "请传入有效的文件");
            return null;
        }
        FileUtil.writeImage2File(bitmap,file);
        return file;
    }

    /**
     * 把sour指定的bitmap压缩scale倍，结果保存到res指定的文件中
     * @param sour 保存原始bitmap的文件
     * @param res 保存结果bitmap的文件
     * @param scale 压缩的倍数
     * @return 保存结果bitmap的文件
     */
    public static File scaleBitmap(File sour,File res , float scale) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = (int) scale;
        Bitmap bitmap = BitmapFactory.decodeFile(sour.getAbsolutePath(), options);
        return saveBitmap(res, bitmap);
    }

    /**
     * 把sour文件中的图片旋转rotation，并把结果保存到res指定的文件中
     * @param context 上下文
     * @param sour 指定原始bitmap所在文件
     * @param res 保存结果bitmap文件
     * @param rotation 旋转的角度
     * @param fitScreen true将会对图片进行适当压缩
     * @return 保存结果bitmap的文件
     */
    public static File rorationBitmap(Context context,File sour,File res ,float rotation,boolean fitScreen) {
        Bitmap bitmap = loadBitmapFromFile(context, sour, fitScreen);
        Matrix matrix = new Matrix();
        matrix.postRotate(rotation);
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        return saveBitmap(res, bitmap);
    }

    /**
     * 把sour文件指定的图片按bounds指定矩形裁剪，结果保存找到res指定的文件中
     * @param context 上下文
     * @param sour 原始bitmap所在的文件
     * @param res 保存结果bitmap的文件
     * @param bounds 裁剪的边界
     * @param fitScreen true将会对图片进行适当的压缩
     * @return 保存结果bitmap的文件
     */
    public static File cropBitmap(Context context,File sour, File res ,Rect bounds,boolean fitScreen) {
        Bitmap src = loadBitmapFromFile(context, sour, fitScreen);
        if(src == null){
            return null;
        }
        int startW = bounds.left;
        int startH = bounds.top;
        int width, height;
        if (startH < 0) {
            startH = 0;
        }
        if (startW < 0) {
            startW = 0;
        }
        if (startH >= src.getHeight() || startW >= src.getWidth()) {
            Log.d(TAG, "======剪切的范围不在允许的范围内======");
            return null;
        }
        width = bounds.right - startW;
        height = bounds.bottom - startH;
        if (width > src.getWidth() || width <= 0) {
            width = src.getWidth() - startW;
        }
        if (height > src.getHeight() || height <= 0) {
            height = src.getHeight() - startH;
        }
        Bitmap resBitmap = Bitmap.createBitmap(src, startW, startH, width, height);
        return saveBitmap(res, resBitmap);
    }
}
