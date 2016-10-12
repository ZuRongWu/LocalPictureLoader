package wzr.com.localpictureloader.core.picture;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;

import wzr.com.localpictureloader.core.picture.editor.ICustomerPictureEditor;
import wzr.com.localpictureloader.core.picture.editor.IPictureEditorCallback;
import wzr.com.localpictureloader.core.picture.editor.SimpleCustomerPicEditorTest;
import wzr.com.localpictureloader.core.picture.tacker.CustomerPicTakerDefault;
import wzr.com.localpictureloader.core.picture.tacker.ICustomerPictureTacker;
import wzr.com.localpictureloader.core.picture.tacker.IPictureTackerCallback;
import wzr.com.localpictureloader.core.picture.tempfile.DefaultTempFileGenerator;
import wzr.com.localpictureloader.core.picture.tempfile.ITempFileGenerator;
import wzr.com.localpictureloader.util.BitmapUtil;
import wzr.com.localpictureloader.util.EventLog;
import wzr.com.localpictureloader.util.StorageUtil;

/**
 * 获取存储在本地的图片
 * 支持可以从相机拍照取得和从系统图片浏览器去的
 * 以文件绝对路径或者是bitmap形式返回
 * 以bitmap形式返回时图片大小不能超过堆上限的1/8如果超过对其进行压缩
 * Created by wuzr on 2016/9/8.
 */
public class LocalPictureLoader {
    /**
     * 从系统相机中获取获取图片
     */
    public static final int TACK_FROM_SYSTEM_CAMERA = 1;
    /**
     * 从系统相册中获取图片
     */
    public static final int TACK_FROM_SYSTEM_GALLERY = 2;
    /**
     * 自定义图片获取途径
     */
    public static final int TACK_FROM_CUSTOMER_TACKER = 7;
    /**
     * 以bitmap的形式返回结果
     */
    public static final int RESULT_FORM_BITMAP = 3;
    /**
     * 以文件绝对路径形式返回结果
     */
    public static final int RESULT_FORM_FILE_PATH = 4;
    /**
     * 调用系统图片编辑器对图片进行处理
     */
    public static final int PROCESS_BITMAP_FROM_SYSTEM_EDITOR = 5;
    /**
     * 自定义图片编辑器
     */
    public static final int PROCESS_BITMAP_FROM_CUSTEMER_EDITOR = 6;
    /**
     * 直接把结果返回不处理
     */
    public static final int NOT_PROCESS_BITMAP = 7;

    private static final boolean DEBUG = true;
    private static final String TAG = "LocalPictureLoader";
    /**
     * BASE_REQUEST_CODE<requestCode<=BASE_REQUEST_CODE + REQUEST_CODE_LENGTH
     * 将会被此类拦截
     */
    public static final int BASE_REQUEST_CODE = 100;
    public static final int REQUEST_CODE_LENGTH = 100;
    private static final int REQUEST_SYSTEM_CAMERA_CODE = BASE_REQUEST_CODE + 1;
    private static final int REQUEST_SYSTEM_GALLERY_CODE = BASE_REQUEST_CODE + 2;
    private static final int REQUEST_SYSTEM_BITMAP_EDITOR_CODE = BASE_REQUEST_CODE + 3;

    private Activity context;
    private LoaderOptions options;
    private PictureLoadCallback callback;
    //记录tempFiles的数量
    private int tempFileCount;
    //保存获取图片过程中临时生成的图片，以便最后把这些图片删除
    private File[] tempFiles = new File[3];
    private File cameraTempFile;
    //标识是否已经处理完
    private boolean isProcessed;

    //requestCodes的最后一个元素的索引
    private static int sLastRequestCodeIndex = -1;
    public static class Result {
        /**
         * 获取图片成功
         */
        public static final int CODE_SUCCESS = 1;
        /**
         * 外部存储器不可用
         */
        public static final int CODE_EXTERNAL_STORAGE_UNAVAILABLE = 2;
        /**
         * 系统图片编辑错误
         */
        public static final int CODE_SYSTEM_PICTURE_EDITOR_ERROR = 3;
        /**
         * 自定义图片编辑错误
         */
        public static final int CODE_CUSTOMER_PICTURE_EDITOR_ERROR = 4;
        /**
         * 自定义图片获取错误
         */
        public static final int CODE_CUSTOMER_PICTURE_TACKER_ERROR = 5;
        /**
         * 系统相册错误
         */
        public static final int CODE_SYSTEM_GALLERY_ERROR = 6;
        /**
         * 系统相机错误
         */
        public static final int CODE_SYSTEM_CAMERA_ERROR = 7;

        public int code;
        public String msg;
        public int resultForm;
        public String[] bitmapPaths;
        public Bitmap[] bitmaps;
    }

    public interface PictureLoadCallback {
        void onResult(Result result);
    }

    /*package*/static int getCacheReqCodeIndex(){
        int cacheIndex = sLastRequestCodeIndex + 1;
        sLastRequestCodeIndex = cacheIndex;
        return cacheIndex;
    }

    /**
     * 获取requestCode
     * cacheIndex[0~8],itemIndex[0~9]
     */
    /*package*/static int getRequestCode(int cacheIndex,int itemIndex){
        if(cacheIndex < 0||itemIndex < 0){
            throw new IllegalArgumentException("请传入正确的index！");
        }
        //101~110预留给本对象使用
        int index = cacheIndex + 1;
        int code;
        code = BASE_REQUEST_CODE + index*10 + itemIndex + 1;
        if(code > BASE_REQUEST_CODE + REQUEST_CODE_LENGTH){
            throw new IllegalArgumentException("没有更多的预留requestCode！");
        }
        return code;
    }

    public void loadPicture(Activity context, LoaderOptions options, PictureLoadCallback callback) {
        EventLog.createEvent("load_picture");
        EventLog.addMarker("load_picture","start");
        this.context = context;
        this.callback = callback;
        if (options == null) {
            this.options = createDeafaultOptions();
        } else {
            if (!(options.tackFrom == TACK_FROM_SYSTEM_GALLERY || options.tackFrom == TACK_FROM_SYSTEM_CAMERA
                  ||options.tackFrom == TACK_FROM_CUSTOMER_TACKER)) {
                options.tackFrom = TACK_FROM_SYSTEM_CAMERA;
            }
            if (!(options.processForm == PROCESS_BITMAP_FROM_SYSTEM_EDITOR || options.processForm == PROCESS_BITMAP_FROM_CUSTEMER_EDITOR)) {
                options.processForm = NOT_PROCESS_BITMAP;
            }
            if (!(options.resultForm == RESULT_FORM_FILE_PATH || options.resultForm == RESULT_FORM_BITMAP)) {
                options.resultForm = RESULT_FORM_FILE_PATH;
            }
            if(options.processForm == PROCESS_BITMAP_FROM_CUSTEMER_EDITOR&&DEBUG){
                options.pictureEditor = createDeafaultPicEditor();
            }
            if(options.tackFrom == TACK_FROM_CUSTOMER_TACKER&&options.pictureTacker == null){
                options.pictureTacker = createDefaultPicTacker();
            }
            if(options.fileGenerator == null){
                options.fileGenerator = createDefaultTempFileGenerator();
            }
            if(options.context == null){
                options.context = this.context;
            }
            this.options = options;
        }
        if (this.options.tackFrom == TACK_FROM_SYSTEM_CAMERA) {
            EventLog.addMarker("load_picture","tack_from_system_camera");
            tackPicFromSystemCamera();
        } else if(this.options.tackFrom == TACK_FROM_SYSTEM_GALLERY){
            EventLog.addMarker("load_picture","tack_from_system_gallery");
            tackPicFromSystemGallery();
        }else {
            EventLog.addMarker("load_picture","tack_from_customer_tacker");
            tackPicFromCustomerTacker();
        }
    }

    public LoaderOptions createDeafaultOptions() {
        LoaderOptions options = new LoaderOptions();
        options.tackFrom = TACK_FROM_SYSTEM_GALLERY;
        options.processForm = NOT_PROCESS_BITMAP;
        options.resultForm = RESULT_FORM_FILE_PATH;
        options.context = this.context;
        return options;
    }

    public ICustomerPictureEditor createDeafaultPicEditor(){
        return new SimpleCustomerPicEditorTest(context);
    }

    public ICustomerPictureTacker createDefaultPicTacker(){
        return new CustomerPicTakerDefault();
    }

    public ITempFileGenerator createDefaultTempFileGenerator(){
        return new DefaultTempFileGenerator(context);
    }

    public void onResult(Intent data, int requestCode, int resultCode) {
        //不可处理此requestCode直接返回
        if(!checkRequestCode(requestCode)){
            EventLog.addMarker("load_picture","can not handle the requestCode");
            return;
        }

        if(routeReqCodeHandle(data,requestCode,resultCode)){
            return;
        }

        if (resultCode != Activity.RESULT_OK) {
            Result result = new Result();
            switch (requestCode){
                case REQUEST_SYSTEM_BITMAP_EDITOR_CODE:
                    result.code = Result.CODE_SYSTEM_PICTURE_EDITOR_ERROR;
                    break;
                case REQUEST_SYSTEM_CAMERA_CODE:
                    result.code = Result.CODE_SYSTEM_CAMERA_ERROR;
                    break;
                case REQUEST_SYSTEM_GALLERY_CODE:
                    result.code = Result.CODE_SYSTEM_GALLERY_ERROR;
                    break;
            }
            result.msg = "获取图片失败";
            deliverResult(result);
            return;
        }
        File resultFile = null;
        if (requestCode == REQUEST_SYSTEM_CAMERA_CODE) {
            EventLog.addMarker("load_picture","parse_camera_result");
            resultFile = parseCameraResult(data);
        } else if (requestCode == REQUEST_SYSTEM_GALLERY_CODE) {
            EventLog.addMarker("load_picture","parse_gallery_result");
            resultFile = parseGalleryResult(data);
        } else if (requestCode == REQUEST_SYSTEM_BITMAP_EDITOR_CODE) {
            EventLog.addMarker("load_picture","parse_system_editor_result");
            resultFile = parseSystemEditorResult(data);
        }
        if (resultFile == null || !resultFile.exists()) {
            Result result = new Result();
            switch (requestCode){
                case REQUEST_SYSTEM_BITMAP_EDITOR_CODE:
                    result.code = Result.CODE_SYSTEM_PICTURE_EDITOR_ERROR;
                    break;
                case REQUEST_SYSTEM_CAMERA_CODE:
                    result.code = Result.CODE_SYSTEM_CAMERA_ERROR;
                    break;
                case REQUEST_SYSTEM_GALLERY_CODE:
                    result.code = Result.CODE_SYSTEM_GALLERY_ERROR;
                    break;
            }
            result.msg = "获取图片失败";
            deliverResult(result);
            if (requestCode == REQUEST_SYSTEM_CAMERA_CODE) {
                addTempFile(resultFile);
            }
            return;
        }
        //系统相机，系统相册，系统图片编辑器都只会返回一张照片
        File[] files = new File[1];
        files[0] = resultFile;
        processResult(files);
    }

    /**
     * requestCode是否对应options.pictureTacker或者options.pictureEditor
     * 如果是则调用它们的onResult方法
     * @return true:改requestCode已被处理完，false：该requestCode未被处理
     */
    private boolean routeReqCodeHandle(Intent data,int requestCode,int resultCode){
        EventLog.addMarker("load_picture","handle requestCode by customer");
        if(options.pictureTacker != null&&options.tackFrom == TACK_FROM_CUSTOMER_TACKER
                &&options.pictureTacker instanceof PictureCallActivityResultAdapter){
            PictureCallActivityResultAdapter pictureTacker = (PictureCallActivityResultAdapter) options.pictureTacker;
            if(requestCode >= pictureTacker.getRequestCode(0)&&requestCode <= pictureTacker.getRequestCode(9)){
                pictureTacker.onResult(data,requestCode,resultCode);
                return true;
            }
        }
        if(options.pictureEditor != null&&options.processForm == PROCESS_BITMAP_FROM_CUSTEMER_EDITOR
                &&options.pictureEditor instanceof PictureCallActivityResultAdapter){
            PictureCallActivityResultAdapter pictureEditor = (PictureCallActivityResultAdapter) options.pictureEditor;
            if(requestCode >= pictureEditor.getRequestCode(0)&&requestCode <= pictureEditor.getRequestCode(9)){
                pictureEditor.onResult(data,requestCode,resultCode);
                return true;
            }
        }
        return false;
    }

    /**
     * 检查是否可以处理此requestCode
     */
    private boolean checkRequestCode(int requestCode){
        return requestCode > BASE_REQUEST_CODE&&requestCode <= BASE_REQUEST_CODE + REQUEST_CODE_LENGTH;
    }

    private void processResult(File[] resultFiles) {
//        this.resultFile = resultFile;
        /*图片编辑完或者不需要编辑直接返回结果,本次请求结束*/
        if (isProcessed || options.processForm == NOT_PROCESS_BITMAP) {
            EventLog.addMarker("load_picture","success");
            Result result = new Result();
            result.code = Result.CODE_SUCCESS;
            if (options.resultForm == RESULT_FORM_FILE_PATH) {
                result.resultForm = RESULT_FORM_FILE_PATH;
                result.bitmapPaths = new String[resultFiles.length];
                for(int i = 0;i < resultFiles.length;i++){
                    result.bitmapPaths[i] = resultFiles[i].getAbsolutePath();
                }
                deliverResult(result);
            } else { //把file转化为图片
                result.bitmaps = new Bitmap[resultFiles.length];
                for(int i = 0;i < resultFiles.length;i++){
                    result.bitmaps[i] = BitmapUtil.loadBitmapFromFile(context, resultFiles[i], options.fitScreen);
                }
                result.resultForm = RESULT_FORM_BITMAP;
               deliverResult(result);
//                addTempFile(resultFile);
            }
            return;
        }
        EventLog.addMarker("load_picture","start_process_pic");
        if (options.processForm == PROCESS_BITMAP_FROM_SYSTEM_EDITOR) {
            EventLog.addMarker("load_picture","process_from_system_editor");
//            addTempFile(resultFile);
            Intent systemEditor = new Intent();
            systemEditor.setAction(Intent.ACTION_EDIT);
            if(resultFiles.length > 1){
                Log.w(TAG,"系统图片编辑器一次只能编辑一张图片，将会放弃除第一张图片外的所有图片");
            }
            systemEditor.setDataAndType(Uri.fromFile(resultFiles[0]), "image/*");
            context.startActivityForResult(systemEditor, REQUEST_SYSTEM_BITMAP_EDITOR_CODE);
        } else { //调用自定义图片编辑器
            ICustomerPictureEditor pictureEditor = options.pictureEditor;
            if(pictureEditor == null){
                throw new IllegalArgumentException("processForm == PROCESS_BITMAP_FROM_CUSTEMER_EDITOR 时" +
                        "pictureEditor不可为null");
            }
            EventLog.addMarker("load_picture","process_from_customer_editor");
            pictureEditor.process(resultFiles, options, new IPictureEditorCallback() {
                @Override
                public void onComplete(File[] outs) {
                    isProcessed = true;
                    for(File out:outs){
                        if (out == null||!out.exists()) {
                            Result result = new Result();
                            result.code = Result.CODE_CUSTOMER_PICTURE_EDITOR_ERROR;
                            result.msg = "获取图片失败";
                            deliverResult(result);
                            EventLog.addMarker("load_picture", "customer_picture_error");
                            return;
                        }
                    }
                    EventLog.addMarker("load_picture","customer_edit_complete");
                    processResult(outs);
                }
            });
        }
    }

    private File parseCameraResult(Intent data) {
        Log.d(TAG, data == null ? "null" : data.toString());
        return cameraTempFile;
    }

    private File parseGalleryResult(Intent data) {
        if (data != null) {
            Uri resultUri = data.getData();
            if (resultUri != null) {
                Log.d(TAG, "uri:" + resultUri.toString());
                ContentResolver resolver = context.getContentResolver();
                Cursor cursor = resolver.query(resultUri, new String[]{MediaStore.Images.Media.DATA}, null, null, null);
                if (cursor != null) {
                    int index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                    if (cursor.getCount() > 0 && cursor.moveToFirst()) {
                        String imgPath = cursor.getString(index);
                        Log.d(TAG, "path:" + imgPath);
                        return new File(imgPath);
                    }
                }
            }
        }
        return null;
    }

    private File parseSystemEditorResult(Intent data) {
        isProcessed = true;
        Log.d(TAG, "data:" + data);
        if (data != null) {
            Uri resultUri = data.getData();
            if (resultUri != null) {
                Log.d(TAG, "uri:" + resultUri.toString());
                ContentResolver resolver = context.getContentResolver();
                Cursor cursor = resolver.query(resultUri, new String[]{MediaStore.Images.Media.DATA}, null, null, null);
                if (cursor != null) {
                    int index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                    if (cursor.getCount() > 0 && cursor.moveToFirst()) {
                        String imgPath = cursor.getString(index);
                        Log.d(TAG, "path:" + imgPath);
                        return new File(imgPath);
                    }
                }
            }
        }
        return null;
    }

    /**
     * 从系统相机中获取图片
     */
    private void tackPicFromSystemCamera() {
        if (!StorageUtil.checkExternalStorageAvaliable(context)) {
            Result result = new Result();
            result.msg = "外部存储不可用，不可拍照";
            result.code = Result.CODE_EXTERNAL_STORAGE_UNAVAILABLE;
            deliverResult(result);
            EventLog.addMarker("load_picture", "system_camera_EXTERNAL_STORAGE_UNAVAILABLE");
            return;
        }
        cameraTempFile = options.fileGenerator.generateTempFile();
        Uri uri = Uri.fromFile(cameraTempFile);
        Log.d(TAG, "path===" + cameraTempFile.getAbsolutePath());
        Log.d(TAG, "uri===" + uri.toString());
        Intent camera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        camera.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        context.startActivityForResult(camera, REQUEST_SYSTEM_CAMERA_CODE);
    }

    /**
     * 从系统相册中获取图片
     */
    private void tackPicFromSystemGallery() {
        if (!StorageUtil.checkExternalStorageAvaliable(context)) {
            Result result = new Result();
            result.msg = "外部存储不可用，无法取得图片！";
            result.code = Result.CODE_EXTERNAL_STORAGE_UNAVAILABLE;
            deliverResult(result);
            EventLog.addMarker("load_picture", "system_gallery_EXTERNAL_STORAGE_UNAVAILABLE");
            return;
        }
        Intent picture = new Intent();
        picture.setType("image/*");
        picture.setAction(Intent.ACTION_PICK);
        context.startActivityForResult(picture, REQUEST_SYSTEM_GALLERY_CODE);
    }

    /**
     * 从自定义图片获取器中获取图片
     */
    private void tackPicFromCustomerTacker(){
        if(options.pictureTacker == null){
            throw new IllegalArgumentException("tackFrom==TACK_FROM_CUSTOMER_TACKER时PictureTacker不能为空");
        }
        options.pictureTacker.tackPicture(options, new IPictureTackerCallback() {
            @Override
            public void onComplete(File[] outs) {
                for (File out : outs) {
                    if (out == null || !out.exists()) {
                        Result result = new Result();
                        result.code = Result.CODE_CUSTOMER_PICTURE_TACKER_ERROR;
                        result.msg = "获取图片失败";
                       deliverResult(result);
                        return;
                    }
                }
                EventLog.addMarker("load_picture", "customer_tacker_complete");
                processResult(outs);
            }
        });
    }

    /**
     * 向调用者递送结果
     */
    private void deliverResult(Result result){
        if(callback != null){
            callback.onResult(result);
        }
        EventLog.addMarker("load_picture","deliver_result");
        EventLog.print("load_picture");
        clear();
    }

    /**
     * 每次获取图片结束后清理资源
     */
    private void clear() {
        /*清除缓存图片*/
        for (File temp : tempFiles) {
            if (temp == null || !temp.exists()) {
                continue;
            }
            if (!temp.delete()) {
                Log.w(TAG, "删除图片" + temp.getAbsolutePath() + "失败！");
            }
        }
        this.context = null;
        this.callback = null;
        this.options = null;
        this.tempFiles = new File[3];
        this.isProcessed = false;
        this.tempFileCount = 0;
        this.cameraTempFile = null;
        sLastRequestCodeIndex = -1;
//        this.resultFile = null;
    }
    /**
    * 此功能尚在调试中
    */
    private void addTempFile(File file) {
        if (true) {
            return;
        }
        while (tempFileCount >= tempFiles.length) {
            File[] newFiles = new File[tempFileCount + 3];
            System.arraycopy(tempFiles, 0, newFiles, 0, tempFiles.length);
            tempFiles = newFiles;
        }
        tempFiles[tempFileCount] = file;
        tempFileCount += 1;
    }
}
