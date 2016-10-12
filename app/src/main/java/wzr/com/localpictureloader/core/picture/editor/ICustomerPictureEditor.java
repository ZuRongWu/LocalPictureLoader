package wzr.com.localpictureloader.core.picture.editor;

import java.io.File;

import wzr.com.localpictureloader.core.picture.LoaderOptions;

/**
 * 自定义图片编辑器接口
 * Created by wuzr on 2016/9/12.
 */
public interface ICustomerPictureEditor {
    void process(File[] srcs, LoaderOptions options, IPictureEditorCallback callback);
}
