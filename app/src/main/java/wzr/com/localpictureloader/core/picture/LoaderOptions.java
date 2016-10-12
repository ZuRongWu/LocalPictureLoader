package wzr.com.localpictureloader.core.picture;

import android.content.Context;
import android.graphics.Rect;

import wzr.com.localpictureloader.core.picture.editor.ICustomerPictureEditor;
import wzr.com.localpictureloader.core.picture.tacker.ICustomerPictureTacker;
import wzr.com.localpictureloader.core.picture.tempfile.ITempFileGenerator;

/**
 * 本地获取图片参数
 * Created by wuzr on 2016/9/12.
 */
public class LoaderOptions {
    public Context context;
    /**
     * 从何处获取图片，当前可为TACK_FROM_SYSTEM_CAMERA（从系统相机获取）;
     * TACK_FROM_SYSTEM_GALLERY(从系统相册获取);TACK_FROM_CUSTOMER_TACKER
     * （从自定义图片获取途径获取图片），默认取值TACK_FROM_SYSTEM_CAMERA
     */
    public int tackFrom;
    /**
     * 结果返回形式,当前可以RESULT_FORM_BITMAP（以bitmap的形式返回）；
     * RESULT_FORM_FILE_PATH（以文件绝对路径形式返回）默认为RESULT_FORM_FILE_PATH
     */
    public int resultForm;
    /**
     * 为获取的图片指定编辑器，当前可为PROCESS_BITMAP_FROM_SYSTEM_EDITOR（
     * 调用系统编辑器对图片进行编辑）；PROCESS_BITMAP_FROM_CUSTEMER_EDITOR（
     * 调用自定义编辑器对图片进行编辑）默认为PROCESS_BITMAP_FROM_SYSTEM_EDITOR
     */
    public int processForm;
    /**
     * 是否对图片进行压缩以适应屏幕,此操作不会使图片模糊显示
     */
    public boolean fitScreen = true;
    /**
     * 临时文件生成器默认为DefaultTempFileGenerator
     */
    public ITempFileGenerator fileGenerator;
    /**
     * 自定义图片编辑器，只有在processForm==PROCESS_BITMAP_FROM_CUSTEMER_EDITOR
     * 时此参数才有效
     */
    public ICustomerPictureEditor pictureEditor;
    /**
     * 自定义图片获取器，只有在tackFrom==TACK_FROM_CUSTOMER_TACKER时此参数才有效
     */
    public ICustomerPictureTacker pictureTacker;
    /**
    * 以下参数只有在tackFrom==TACK_FROM_SYSTEM_CAMERA或者
     * processForm==PROCESS_BITMAP_FROM_CUSTEMER_EDITOR时有效
    */
    /**
     * 调用者可以任意设置此参数，此参数将传递给CustomerPictureEditor和CustomerPictureTacker
     */
    public Object extra;
    public float scale;
    public Rect cropBounds;
    public float rotation;
}
