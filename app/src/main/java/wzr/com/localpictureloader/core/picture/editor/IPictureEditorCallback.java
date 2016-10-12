package wzr.com.localpictureloader.core.picture.editor;

import java.io.File;

/**
 * 图片编辑回调接口
 * Created by wuzr on 2016/9/12.
 */
public interface IPictureEditorCallback {
    void onComplete(File[] outs);
}
