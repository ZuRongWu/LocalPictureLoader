package wzr.com.localpictureloader.core.picture.editor;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.File;

import wzr.com.localpictureloader.core.picture.LoaderOptions;
import wzr.com.localpictureloader.util.BitmapUtil;

/**
 * 测试自定义图片编辑器
 * Created by wuzr on 2016/9/12.
 */
public class SimpleCustomerPicEditorTest implements ICustomerPictureEditor{
    private static final String TAG = "CustomerPicEditorTest";
    private IPictureEditorCallback callback;
    private File source;
    private LoaderOptions options;
    private Context context;
    private Handler handler = new PicEditorHandler(this);

    private static class PicEditorHandler extends Handler{
        private static final int EDIT_COMPLETE = 1;

        private SimpleCustomerPicEditorTest editor;

        private PicEditorHandler(SimpleCustomerPicEditorTest editor){
            super();
            this.editor = editor;
        }

        @Override
        public void handleMessage(Message msg) {
            if(msg.what == EDIT_COMPLETE){
                File result = (File) msg.obj;
                File[] results = new File[1];
                results[0] = result;
                if(editor.callback != null){
                    editor.callback.onComplete(results);
                }
            }
        }
    }

    private Runnable editorTask = new Runnable() {
        @Override
        public void run() {
            File out = null;
            if (options.cropBounds != null) {
                out = BitmapUtil.cropBitmap(context, source, options.fileGenerator.generateTempFile(), options.cropBounds, options.fitScreen);
                if(out == null){
                    Message msg = handler.obtainMessage(PicEditorHandler.EDIT_COMPLETE);
                    msg.obj = null;
                    handler.sendMessage(msg);
                    return;
                }
            }
            if (options.rotation != 0) {
                out = BitmapUtil.rorationBitmap(context,out,options.fileGenerator.generateTempFile(), options.rotation,options.fitScreen);
                if(out == null){
                    Message msg = handler.obtainMessage(PicEditorHandler.EDIT_COMPLETE);
                    msg.obj = null;
                    handler.sendMessage(msg);
                    return;
                }
            }
            if (options.scale != 0) {
                out = BitmapUtil.scaleBitmap(out,options.fileGenerator.generateTempFile(), options.scale);
                if(out == null){
                    Message msg = handler.obtainMessage(PicEditorHandler.EDIT_COMPLETE);
                    msg.obj = null;
                    handler.sendMessage(msg);
                    return;
                }
            }
            Message msg = handler.obtainMessage(PicEditorHandler.EDIT_COMPLETE);
            msg.obj = out;
            handler.sendMessage(msg);
        }
    };

    public SimpleCustomerPicEditorTest(Context context){
        this.context = context;
    }
    @Override
    public void process(File[] srcs, LoaderOptions options,IPictureEditorCallback callback) {
        if(srcs.length > 0){
            Log.w(TAG,"SimpleCustomerPicEditorTest 一次只能编辑一张图片");
        }
        this.source = srcs[0];
        this.options = options;
        this.callback = callback;
        Thread thread = new Thread(editorTask);
        thread.start();
    }
}
