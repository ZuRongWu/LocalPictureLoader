package wzr.com.localpictureloader.core.picture.tacker;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import java.io.File;

import wzr.com.localpictureloader.core.picture.LoaderOptions;
import wzr.com.localpictureloader.core.picture.PictureCallActivityResultAdapter;
import wzr.com.localpictureloader.util.EventLog;

/**
 *
 * Created by wuzr on 2016/9/30.
 */
public class CustomerPicTakerDefault extends PictureCallActivityResultAdapter implements ICustomerPictureTacker{
    private static final String TAG = "CustomerPicTaker";
    private IPictureTackerCallback tackerCallback;
    @Override
    public void tackPicture(LoaderOptions options, IPictureTackerCallback callback) {
        EventLog.addMarker("load_picture", "tack_from_default_customer_tacker");
        Context context = options.context;
        if(!(context instanceof Activity)){
            throw new IllegalArgumentException("context必须为activity！！！");
        }
        this.tackerCallback = callback;
        Activity host = (Activity) context;
        Intent intent = new Intent(host,CustomerPicChooseActivity.class);
        host.startActivityForResult(intent,getRequestCode(1));
    }

    @Override
    public void onResult(Intent data, int requestCode, int resultCode) {
        EventLog.addMarker("load_picture","choose_picture_result");
        if(requestCode == getRequestCode(1)){
            String[] resultPaths = data.getStringArrayExtra(CustomerPicChooseActivity.RESLT_PATH_ARRAY);
           File[] resultFiles = new File[resultPaths.length];
            for (int i = 0;i < resultPaths.length; i++) {
                String resultPath = resultPaths[i];
                File f = new File(resultPath);
                resultFiles[i] = f;
            }
            tackerCallback.onComplete(resultFiles);
        }
    }
}
