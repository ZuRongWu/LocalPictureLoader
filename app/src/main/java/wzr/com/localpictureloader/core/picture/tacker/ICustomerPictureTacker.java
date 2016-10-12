package wzr.com.localpictureloader.core.picture.tacker;

import wzr.com.localpictureloader.core.picture.LoaderOptions;

/**
 * 自定义图片获取器接口
 * Created by wuzr on 2016/9/12.
 */
public interface ICustomerPictureTacker {
    void tackPicture(LoaderOptions options, IPictureTackerCallback callback);
}
