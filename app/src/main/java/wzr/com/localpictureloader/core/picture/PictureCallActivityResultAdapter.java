package wzr.com.localpictureloader.core.picture;

import android.content.Intent;

/**
 * 增加处理onActivityResult()逻辑,
 * 调用startActivityForResult()调用getRequestCode(int index)
 * 动态获取requestCode，参数index用于区分同属于此对象中的不同
 * requestCode,对于给定的index，在此对象的存活期间每次调用
 * getRequestCode(int index)获得的requestCode都相同
 * Created by wuzr on 2016/9/12.
 */
public abstract class PictureCallActivityResultAdapter{
    private int cacheReqCodeIndex = -1;
    private int getCacheReqCodeIndex(){
        return LocalPictureLoader.getCacheReqCodeIndex();
    }

    public abstract void onResult(Intent data,int requestCode,int resultCode);

    /**
     * 调用startActivityForResult()调用此方法获取requestCode
     */
    public final int getRequestCode(int index){
        if(cacheReqCodeIndex == -1){
            cacheReqCodeIndex = getCacheReqCodeIndex();
        }
        return LocalPictureLoader.getRequestCode(cacheReqCodeIndex,index);
    }
}
