package wzr.com.localpictureloader.core.picture.tempfile;

import android.content.Context;

import java.io.File;
import java.io.IOException;

import wzr.com.localpictureloader.util.FileUtil;

/**
 * 默认临时文件生成器
 * Created by wuzr on 2016/9/12.
 */
public class DefaultTempFileGenerator implements ITempFileGenerator{
    private Context context;

    public DefaultTempFileGenerator(Context context){
        this.context = context;
    }

    @Override
    public File generateTempFile() {
        File file = FileUtil.TempFileUtil.obtainTempFile(context,"",true);
        try {
            if (!file.exists()&&!file.createNewFile()&&!file.setWritable(true)) {
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return file;
    }
}
