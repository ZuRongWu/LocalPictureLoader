package wzr.com.localpictureloader.core.picture.tacker;

import java.io.File;

/**
 * 自定义图片获取回调接口
 * Created by wuzr on 2016/9/12.
 */
public interface IPictureTackerCallback {
    void onComplete(File[] outs);
}
