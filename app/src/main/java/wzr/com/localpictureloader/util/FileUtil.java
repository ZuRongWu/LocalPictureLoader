package wzr.com.localpictureloader.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * 文件操作工具类
 * 概述：
 * 把app的存储划分为缓存目录(.../${package_name}/cache/)；临时文件目录
 * (.../${package_name}/temp/)；配置文件目录(.../${package_name}/config/)；
 * 用于保存图片文件目录(.../${package_name}/image/)；普通文件目录
 * (.../${package_name}/file/)。各种目录都有对应的内部路径和外部存储路径。
 * 对相关文件操作时可以指定preferExternal来指定要访问的是内部路径还是外部存储路径。
 * 默认情况下访问外部存储路径，如果外面存储不可用时都访问内部路径。.../${package_name}/cache/目录
 * 和.../${package_name}/temp/不应该保存会影响程序运行的结果的文件，只能用来保存临时文件
 * 和缓存文件，程序中调用clear()方法将会删除这两个目录下的所以文件
 * Created by wuzr on 2016/9/23.
 */
public class FileUtil {

    private static final String TAG = "FileUtil";

    /**
     * 缓存相关操作start
     * 1.添加普通缓存数据到指定文件
     * 2.添加字符串缓存到指定文件
     * 3.添加图片缓存到指定文件
     * 4.获取指定文件中的缓存数据
     * 5.获取指定文件中的字符串缓存数据
     * 6.获取指定文件中的图片缓存数据
     */
    public static class CacheFileUtil {
        /**
         * 把data缓存到fileName指定的文件中
         *
         * @param context        上下文
         * @param data           要缓存的内容
         * @param fileName       缓存文件
         * @param preferExternal true则优先缓存到外部存储
         * @return null操作失败，成功返回目标文件的绝对路径
         */
        public static String addCache(Context context, byte[] data, String fileName, boolean preferExternal) {
            if (data == null) {
                return null;
            }
            File cacheFile = obtainCacheFile(context, fileName, preferExternal);
            if (cacheFile == null) {
                return null;
            }
            if (writeContent2File(data, cacheFile)) {
                return cacheFile.getAbsolutePath();
            }
            return null;
        }

        public static String addCache(Context context, byte[] data) {
            return addCache(context, data, null, true);
        }

        /**
         * 把字符串缓存到fileName指定的文件中
         *
         * @param context        上下文
         * @param data           将要缓存的字符串
         * @param fileName       目标文件的文件名，不存在则创建一个新文件
         * @param preferExternal true则优先使用外部存储
         * @return null操作失败，成功则返回目标文件的绝对路径
         */
        public static String addStringCache(Context context, String data, String fileName, boolean preferExternal) {
            if (data == null || data.equals("")) {
                return null;
            }
            File cacheFile = obtainCacheFile(context, fileName, preferExternal);
            if (cacheFile == null) {
                return null;
            }
            if (writeString2File(data, cacheFile)) {
                return cacheFile.getAbsolutePath();
            }
            return null;
        }

        public static String addStringCache(Context context, String data) {
            return addStringCache(context, data, null, true);
        }

        /**
         * 把图片bitmap缓存到fileName指定的文件中
         *
         * @param context        上下文
         * @param bitmap         将要缓存的图片
         * @param fileName       目标的文件的名称
         * @param preferExternal true优先缓存到外部存储
         * @return null操作失败，操作成功则返回缓存文件的绝对路径
         */
        public static String addImageCache(Context context, Bitmap bitmap, String fileName, boolean preferExternal) {
            if (bitmap == null || bitmap.isRecycled()) {
                return null;
            }
            File cacheFile = obtainCacheFile(context, fileName, preferExternal);
            if (cacheFile == null) {
                return null;
            }
            if (writeImage2File(bitmap, cacheFile)) {
                return cacheFile.getAbsolutePath();
            }
            return null;
        }

        /**
         * 从filePath指定的文件中获取缓存数据
         *
         * @param filePath 指定目标文件
         * @return 获取到的缓存内容
         */
        public static byte[] getCache(Context context, String filePath) {
            File cacheFile = findCacheFile(context, filePath, true);
            if (cacheFile == null) {
                return null;
            }
            return readContentFromFile(cacheFile);
        }

        /**
         * 从指定的文件获取缓存的字符串
         *
         * @param filePath 目标文件的绝对路径
         * @return 获取到的缓存字符串
         */
        public static String getStringCache(Context context, String filePath) {
            File cacheFile = findCacheFile(context, filePath, true);
            if (cacheFile == null) {
                return null;
            }
            return readStringFromFile(cacheFile);
        }

        public static Bitmap getImageCache(Context context, String filePath, BitmapFactory.Options options, int desireWidth, int desireHeight) {
            File cacheFile = findCacheFile(context, filePath, true);
            if (cacheFile == null) {
                return null;
            }
            return readImageFromFile(cacheFile, options, desireWidth, desireHeight);
        }

        public static boolean clear(Context context) {
            File[] dirs = new File[2];
            dirs[0] = StorageUtil.getCacheDir(context, false);
            dirs[1] = StorageUtil.getExternalCacheDir(context);
            return FileUtil.clear(context, false, false, dirs);
        }

        private static File obtainCacheFile(Context context, String fileName, boolean preferExternal) {
            File ownerDir = StorageUtil.getCacheDir(context, false);
            File externalDir = StorageUtil.getExternalCacheDir(context);
            return obtainFile(ownerDir, externalDir, fileName, preferExternal);
        }

        /**
         * 找到缓存文件
         *
         * @param fileDesc 1.为文件的绝对路劲2.为文件名，这种情况先外部存储中找
         *                 如果在外部存储中没找到再到内部存储中找
         * @return 找到的缓存文件
         */
        private static File findCacheFile(Context context, String fileDesc, boolean preferExternal) {
            File ownerDir = StorageUtil.getCacheDir(context, false);
            File externalDir = StorageUtil.getExternalCacheDir(context);
            return findFile(ownerDir, externalDir, fileDesc, preferExternal);
        }
    }

    /**
     * 临时文件相关操作start
     * 1.把数据保存为临时文件
     * 2.把字符串保存为临时文件
     * 3.把图片保存为临时文件
     * 4.从指定临时文件中获取数据
     * 5.从临时文件中获取字符串
     * 6.从临时文件获取图片
     */
    public static class TempFileUtil {
        /**
         * 把数据保存为fileName指定的临时文件
         *
         * @param context        上下文
         * @param data           将要保存的数据
         * @param fileName       目标文件名称
         * @param preferExternal true优先选择外部存储
         * @return null操作失败，成功返回临时文件的绝对路劲
         */
        public static String addTemp(Context context, byte[] data, String fileName, boolean preferExternal) {
            if (data == null) {
                return null;
            }
            File tempFile = obtainTempFile(context, fileName, preferExternal);
            if (tempFile == null) {
                return null;
            }
            if (writeContent2File(data, tempFile)) {
                return tempFile.getAbsolutePath();
            }
            return null;
        }

        public static String addTemp(Context context, byte[] data) {
            return addTemp(context, data, null, true);
        }

        /**
         * 把字符串data保存为fileName指定的临时文件
         *
         * @param context        上下文
         * @param data           将要保存为临时文件的字符串
         * @param fileName       目标文件的名称
         * @param preferExternal true优先使用外部存储
         * @return null操作失败，操作成功返回目标文件的绝对路径
         */
        public static String addStringTemp(Context context, String data, String fileName, boolean preferExternal) {
            if (data == null || data.equals("")) {
                return null;
            }
            File tempFile = obtainTempFile(context, fileName, preferExternal);
            if (tempFile == null) {
                return null;
            }
            if (writeString2File(data, tempFile)) {
                return tempFile.getAbsolutePath();
            }
            return null;
        }

        public static String addStringTemp(Context context, String data) {
            return addStringTemp(context, data, null, true);
        }

        /**
         * 把图片bitmap保存在fileName指定的临时文件中
         *
         * @param context        上下文
         * @param bitmap         将要保存的图片
         * @param fileName       目标文件的名称
         * @param preferExternal true优先使用外部存储
         * @return null操作失败，操作成功返回目标文件的绝对路径
         */
        public static String addImageTemp(Context context, Bitmap bitmap, String fileName, boolean preferExternal) {
            if (bitmap == null || bitmap.isRecycled()) {
                return null;
            }
            File tempFile = obtainTempFile(context, fileName, preferExternal);
            if (tempFile == null) {
                return null;
            }
            if (writeImage2File(bitmap, tempFile)) {
                return tempFile.getAbsolutePath();
            }
            return null;
        }

        public static String addImageTemp(Context context, Bitmap bitmap) {
            return addImageTemp(context, bitmap, null, true);
        }

        /**
         * 从指定的临时文件中获取数据
         *
         * @param context  上下文
         * @param filePath 目标文件的绝对路径
         * @return 返回从指定临时文件中获取到的数据
         */
        public static byte[] getTemp(Context context, String filePath) {
            File tempFile = findTempFile(context, filePath, true);
            if (tempFile == null) {
                return null;
            }
            return readContentFromFile(tempFile);
        }

        /**
         * 从指定临时文件中获取字符串
         *
         * @param context  上下文
         * @param filePath 指定临时文件的绝对路径
         * @return 指定临时文件中的字符串
         */
        public static String getStringTemp(Context context, String filePath) {
            File tempFile = findTempFile(context, filePath, true);
            if (tempFile == null) {
                return null;
            }
            return readStringFromFile(tempFile);
        }

        /**
         * 从指定的临时文件获取图片
         *
         * @param context  上下文
         * @param filePath 指定临时文件的绝对路径
         * @param options  获取图片的参数
         * @return 从临时文件中取得的图片
         */
        public static Bitmap getImageTemp(Context context, String filePath, BitmapFactory.Options options, int desireWidth, int desireHeight) {
            File tempFile = findTempFile(context, filePath, true);
            if (tempFile == null) {
                return null;
            }
            return readImageFromFile(tempFile, options, desireWidth, desireHeight);
        }

        public static boolean clear(Context context) {
            File[] dirs = new File[2];
            dirs[0] = StorageUtil.getOwnerImageDir(context);
            dirs[1] = StorageUtil.getExternalTempDir(context);
            return FileUtil.clear(context, false, false, dirs);
        }

        public static File obtainTempFile(Context context, String fileName, boolean preferExternal) {
            File ownerDir = StorageUtil.getOwnerTempDir(context);
            File externalDir = StorageUtil.getExternalTempDir(context);
            return obtainFile(ownerDir, externalDir, fileName, preferExternal);
        }

        /**
         * 找到临时文件
         *
         * @param fileDesc       1.为文件的绝对路劲2.为文件名
         * @param preferExternal true优先在外部存储中查找，没找到的情况才在内部存储中查找
         * @return 找到的缓存文件
         */
        private static File findTempFile(Context context, String fileDesc, boolean preferExternal) {
            File ownerDir = StorageUtil.getOwnerTempDir(context);
            File externalDir = StorageUtil.getExternalTempDir(context);
            return findFile(ownerDir, externalDir, fileDesc, preferExternal);
        }
    }

    /**
     * 图片文件的工具
     * 1.把图片保存为到指定文件
     * 2.创建新的文件夹
     * 3.从指定文件中获取图片
     */

    public static class ImageFileUtil {
        public static boolean saveImage2File(Bitmap bitmap, File file) {
            return writeImage2File(bitmap, file);
        }

        public static File makeImageDir(Context context, String dirPath, boolean preferExternal) {
            if (dirPath == null || dirPath.equals("")) {
                return null;
            }
            File externalDir = StorageUtil.getExternalImageDir(context);
            File ownerDir = StorageUtil.getOwnerImageDir(context);
            return makeDir(ownerDir, externalDir, dirPath, preferExternal);
        }

        public static File findImageDir(Context context, String dirDesc, boolean preferExternal) {
            if (dirDesc == null || dirDesc.equals("")) {
                return null;
            }
            File externalDir = StorageUtil.getExternalImageDir(context);
            File ownerDir = StorageUtil.getOwnerImageDir(context);
            return findDir(ownerDir, externalDir, dirDesc, preferExternal);
        }

        public static Bitmap getImageFromFile(Context context, String fileDesc, int desireWidth, int desireHeight, boolean preferExternal) {
            File file = findImageFile(context, fileDesc, preferExternal);
            if (file == null || !file.exists()) {
                return null;
            }
            return readImageFromFile(file, null, desireWidth, desireHeight);
        }

        public static File obtainImageFile(Context context, String dirPath, String fileName, boolean preferExternal) {
            File ownerDir = StorageUtil.getOwnerImageDir(context);
            File externalDir = StorageUtil.getExternalImageDir(context);
            if (!(dirPath == null) && !dirPath.equals("")) {
                if (externalDir != null) {
                    externalDir = new File(externalDir, dirPath);
                    if (!externalDir.exists()) {
                        if (!externalDir.mkdirs()) {
                            Log.d(TAG, "创建目录" + externalDir.getAbsolutePath() + "失败");
                        }
                    }
                }
                if (ownerDir != null) {
                    ownerDir = new File(ownerDir, dirPath);
                    if (!ownerDir.exists()) {
                        if (!ownerDir.mkdirs()) {
                            Log.d(TAG, "创建目录" + externalDir.getAbsolutePath() + "失败");
                        }
                    }
                }
            }
            return obtainFile(ownerDir, externalDir, fileName, preferExternal);
        }

        public static File obtainImageFile(Context context, String dirPath, String fileName) {
            return obtainImageFile(context, dirPath, fileName, true);
        }

        public static File obtainImageFile(Context context, String fileDesc, boolean preferExternal) {
            int lastSeparatorIndex = fileDesc.lastIndexOf(File.separator);
            String dirPath = fileDesc.substring(0, lastSeparatorIndex);
            String fileName = fileDesc.substring(lastSeparatorIndex + 1);
            return obtainImageFile(context, dirPath, fileName, preferExternal);
        }

        public static File obtainImageFile(Context context, String fileDesc) {
            return obtainImageFile(context, fileDesc, true);
        }

        public static File findImageFile(Context context, String fileDesc, boolean preferExternal) {
            File ownerDir = StorageUtil.getOwnerImageDir(context);
            File externalDir = StorageUtil.getExternalImageDir(context);
            return findFile(ownerDir, externalDir, fileDesc, preferExternal);
        }

        public static File findImageFile(Context context, String fileDesc) {
            return findImageFile(context, fileDesc, true);
        }
    }

    /**
     * 普通文件的工具
     * 1.创建新的文件夹
     * 2.给定文件路径获得文件
     * 3.查找给定路径的文件
     */
    public static class NormalFileUtil {
        /**
         * 创建文件夹
         *
         * @param context        上下文
         * @param dirPath        相对于.../${package_name}/file/，例如dirName = example/test/则创建文件夹.../${package_name}/file/example/test/
         * @param preferExternal true则优先在外部存储中创建
         * @return 创建成功返回创建的目录，创建失败返回null
         */
        public static File makeFileDir(Context context, String dirPath, boolean preferExternal) {
            if (dirPath == null || dirPath.equals("")) {
                return null;
            }
            File externalDir = StorageUtil.getExternalFileDir(context);
            File ownerDir = StorageUtil.getOwnerFileDir(context);
            return makeDir(ownerDir,externalDir,dirPath,preferExternal);
        }

        public static File findFileDir(Context context,String dirDesc,boolean preferExternal){
            if (dirDesc == null || dirDesc.equals("")) {
                return null;
            }
            File externalDir = StorageUtil.getExternalFileDir(context);
            File ownerDir = StorageUtil.getOwnerFileDir(context);
            return findDir(ownerDir,externalDir,dirDesc,preferExternal);
        }

        /**
         * 获取文件，若存在dirPath和fileName指定文件时返回此文件，否则创建新的文件并返回
         *
         * @param context        上下文
         * @param dirPath        相对于.../${package_name}/file/的路径
         * @param fileName       文件的名称
         * @param preferExternal true优先考虑外部存储
         * @return 获得的文件
         */
        public static File obtainNormalFile(Context context, String dirPath, String fileName, boolean preferExternal) {
            File ownerDir = StorageUtil.getOwnerFileDir(context);
            File externalDir = StorageUtil.getExternalFileDir(context);
            if (!(dirPath == null) && !dirPath.equals("")) {
                if (externalDir != null) {
                    externalDir = new File(externalDir, dirPath);
                    if (!externalDir.exists()) {
                        if (!externalDir.mkdirs()) {
                            Log.d(TAG, "创建目录" + externalDir.getAbsolutePath() + "失败");
                        }
                    }
                }
                if (ownerDir != null) {
                    ownerDir = new File(ownerDir, dirPath);
                    if (!ownerDir.exists()) {
                        if (!ownerDir.mkdirs()) {
                            Log.d(TAG, "创建目录" + ownerDir.getAbsolutePath() + "失败");
                        }
                    }
                }
            }
            return obtainFile(ownerDir, externalDir, fileName, preferExternal);
        }

        public static File obtainNormalFile(Context context, String dirPath, String fileName) {
            return obtainNormalFile(context, dirPath, fileName, true);
        }

        public static File obtainNormalFile(Context context, String fileDesc, boolean preferExternal) {
            int lastSeparatorIndex = fileDesc.lastIndexOf(File.separator);
            String dirPath = fileDesc.substring(0, lastSeparatorIndex);
            String fileName = fileDesc.substring(lastSeparatorIndex + 1);
            return obtainNormalFile(context, dirPath, fileName, preferExternal);
        }

        public static File obtainNormalFile(Context context, String fileDesc) {
            return obtainNormalFile(context, fileDesc, true);
        }

        /**
         * 查找fileDesc指定的文件，fileDesc可取值为/example/test.txt，或者是绝对路径
         *
         * @param context        上下文
         * @param fileDesc       描述要查找的文件
         * @param preferExternal true优先在外部存储中查找
         * @return 找到的文件
         */
        public static File findNormalFile(Context context, String fileDesc, boolean preferExternal) {
            File ownerDir = StorageUtil.getOwnerFileDir(context);
            File externalDir = StorageUtil.getExternalFileDir(context);
            return findFile(ownerDir, externalDir, fileDesc, preferExternal);
        }

        public static File findNormalFile(Context context, String fileDesc) {
            return findNormalFile(context, fileDesc, true);
        }
    }

    /**
     * 把data写到file指定的文件中
     *
     * @param data 要写入文件的内容
     * @param file 目标文件
     * @return true操作成功，false操作失败
     */
    public static boolean writeContent2File(byte[] data, File file) {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(file);
            out.write(data);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            if (out != null)
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
        return true;
    }

    /**
     * 从文件file中读取内容
     *
     * @param file 目标文件
     * @return 读取到的内容
     */
    public static byte[] readContentFromFile(File file) {
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            byte[] data = new byte[in.available()];
            if (in.read(data, 0, data.length) == -1) {
                return null;
            }
            return data;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 从file指定的文件中读取字符串
     *
     * @param file 目标文件
     * @return file中的字符串
     */
    public static String readStringFromFile(File file) {
        FileReader reader = null;
        try {
            reader = new FileReader(file);
            char[] data = new char[1024];
            int len;
            String str = "";
            while ((len = reader.read(data)) != -1) {
                str = String.valueOf(data, 0, len) + str;
            }
            return str;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                if (reader != null)
                    reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 从文件中获取图片，调用者可以指定一个期望的宽高以节省所需内存
     *
     * @param file         目标文件
     * @param options      传递给BitmapFactory.decodeFile()函数
     * @param desireWidth  期望的宽度,<=0时不做限制
     * @param desireHeight 期望的高度，<=0时不做限制
     * @return bitmap
     */
    public static Bitmap readImageFromFile(File file, BitmapFactory.Options options, int desireWidth, int desireHeight) {
        if (options == null)
            options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        int acWidth, acHeight;
        BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        acWidth = options.outWidth;
        acHeight = options.outHeight;
        boolean needScale = true;
        boolean fitWidth = false;
        if (desireHeight <= 0) {
            fitWidth = true;
        }
        if (desireWidth <= 0) {
            fitWidth = false;
        }
        if (desireHeight <= 0 && desireWidth <= 0) {
            needScale = false;
        }
        if (desireHeight > 0 && desireWidth > 0) {
            fitWidth = desireWidth / desireHeight < 1;
        }
        if (needScale) {
            if (fitWidth) {
                if (acWidth <= desireWidth) {
                    needScale = false;
                }
            } else {
                if (acHeight <= desireHeight) {
                    needScale = false;
                }
            }
        }
        if (needScale) {
            int scale = 1;
            if (fitWidth) {
                while (acWidth > desireWidth) {
                    scale *= 2;
                    acWidth = acWidth / scale;
                }
            } else {
                while (acHeight > desireHeight) {
                    scale *= 2;
                    acHeight = acHeight / scale;
                }
            }
            options.inSampleSize = scale;
        }
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(file.getAbsolutePath(), options);
    }

    /**
     * 把字符串data写到文件file中
     *
     * @param data 将要写入的字符串
     * @param file 目标文件
     * @return true操作成功，false操作失败
     */
    public static boolean writeString2File(String data, File file) {
        FileWriter writer = null;
        try {
            writer = new FileWriter(file);
            writer.write(data);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            if (writer != null)
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
        return true;
    }

    /**
     * 把图片bitmap写到file文件中
     *
     * @param bitmap 将要写入的文件
     * @param file   目标文件
     * @return true操作成功，false操作失败
     */
    public static boolean writeImage2File(Bitmap bitmap, File file) {
        try {
            FileOutputStream fos = new FileOutputStream(file);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            byte[] bytes = stream.toByteArray();
            fos.write(bytes);
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * 生成文件名称，当前时间的毫秒数
     *
     * @return 文件名称
     */
    public static String generateFileName() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd H:m:ss", Locale.getDefault());
        return dateFormat.format(Calendar.getInstance().getTime());
    }

    /**
     * 在指定的根目录中(ownerDir,externalDir)中查找fileDesc描述的文件
     *
     * @param ownerDir       内部存储根目录
     * @param externalDir    外部存储跟目录
     * @param fileDesc       描述要查找的文件
     * @param preferExternal true优先在外部存储中查找
     * @return 找到的文件
     */
    public static File findFile(File ownerDir, File externalDir, String fileDesc, boolean preferExternal) {
        if (fileDesc == null || fileDesc.equals("")) {
            return null;
        }
        File file = null;
        if ((ownerDir != null && fileDesc.startsWith(ownerDir.getAbsolutePath()))
                || (externalDir != null && fileDesc.startsWith(externalDir.getAbsolutePath()))) {
            file = new File(fileDesc);
            if (file.exists()) {
                return file;
            } else {
                return null;
            }
        }
        if (externalDir != null && preferExternal) {
            //在外部存储中查找
            file = findFileInternal(externalDir, fileDesc);
        }
        if (file != null) {
            return file;
        }
        //在内部存储中查找
        file = findFileInternal(ownerDir, fileDesc);
        return file;
    }

    public static File findFile(File ownerDir, File externalDir, String fileDesc) {
        return findFile(ownerDir, externalDir, fileDesc, true);
    }

    private static File findFileInternal(File root, String fileDesc) {
        if (root.getAbsolutePath().endsWith(fileDesc)) {
            return root;
        }
        if (!root.isDirectory()) {
            return null;
        }
        File res = null;
        for (File f : root.listFiles()) {
            res = findFileInternal(f, fileDesc);
            if (res != null) {
                return res;
            }
        }
        return res;
    }

    /**
     * 在根目录中获得名称为fileName文件，如果文件存在则返回此文件，不存在则先创建名称为fileName的文件在将其返回
     *
     * @param ownerDir       内部存储根目录
     * @param externalDir    外部存储根目录
     * @param fileName       将要获得的文件的名称
     * @param preferExternal true优先在外部存储获得
     * @return 获得的文件
     */
    public static File obtainFile(File ownerDir, File externalDir, String fileName, boolean preferExternal) {
        if (fileName == null || fileName.equals("")) {
            fileName = generateFileName();
        }
        File dir = null;
        if (preferExternal) {
            dir = externalDir;
        }
        if (dir == null) {
            dir = ownerDir;
        }
        if (dir == null) {
            return null;
        }
        File file = new File(dir, fileName);
        if (file.exists()) {
            return file;
        }
        try {
            if (!file.createNewFile()) {
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return file;
    }

    public static File obtainFile(File ownerDir, File externalDir, String fileName) {
        return obtainFile(ownerDir, externalDir, fileName, true);
    }

    /**
     * 从from拷贝文件到to
     *
     * @param from   源文件
     * @param to     目标文件
     * @param isCrop true拷贝完成之后删除源文件
     * @return true操作成功，false操作失败
     */
    private static boolean copyInternal(File from, File to, boolean isCrop) {
        if (from == null || !from.exists()) {
            return false;
        }
        if (to == null) {
            return false;
        }
        if (!to.exists()) {
            try {
                if (!to.createNewFile())
                    return false;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        FileInputStream in = null;
        FileOutputStream out = null;
        try {
            in = new FileInputStream(from);
            out = new FileOutputStream(to);
            byte[] buffer = new byte[1024];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
            if (isCrop) {
                if (!from.delete())
                    return false;
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static boolean copy(File from, File to) {
        return copyInternal(from, to, false);
    }

    public static boolean crop(File from, File to) {
        return copyInternal(from, to, true);
    }

    /**
     * 清除缓存和临时文件和调用端指定的文件
     *
     * @param context      上下文
     * @param isClearTemp  true删除临时文件
     * @param isClearCache true删除缓存文件
     * @param files        要删除的文件
     * @return 操作成功返回true，否则返回false
     */
    public static boolean clear(Context context, boolean isClearTemp, boolean isClearCache, File... files) {
        boolean res = true;
        if (isClearTemp) {
            res = TempFileUtil.clear(context);
        }
        if (isClearCache) {
            res = CacheFileUtil.clear(context) && res;
        }
        if (files == null) {
            return res;
        }
        for (File file : files) {
            if (file == null || !file.exists()) {
                continue;
            }
            res = file.delete() && res;
        }
        return res;
    }

    public static boolean clear(Context context) {
        return clear(context, true, true);
    }

    /**
     * 统计file指定的文件或者文件夹下所有文件的大小
     *
     * @param file 要统计的文件或者文件夹
     * @return 总大小单位为MB
     */
    public static double caculateFileLength(File file) {
        if (file == null || !file.exists()) {
            return 0;
        }
        if (!file.isDirectory()) {
            return file.length();
        }
        long length = 0;
        for (File f : file.listFiles()) {
            length += caculateFileLength(f);
        }
        //把length转换成MB
        return (double) length / (1024 * 1024);
    }

    /**
     * 创建文件目录
     */
    public static File makeDir(File ownerDir, File externalDir, String dirDesc, boolean preferExternal) {
        if (preferExternal) {
            if (externalDir != null) {
                File dir = new File(externalDir, dirDesc);
                if (dir.exists() || dir.mkdirs()) {
                    return dir;
                }
            }
        }
        File dir = new File(ownerDir, dirDesc);
        if (dir.exists() || dir.mkdirs()) {
            return dir;
        }
        return null;
    }

    public static File findDir(File ownerDir, File externalDir, String dirDesc, boolean preferExternal) {
        return findFile(ownerDir, externalDir, dirDesc, preferExternal);
    }
}
