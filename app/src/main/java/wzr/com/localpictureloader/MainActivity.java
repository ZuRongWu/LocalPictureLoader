package wzr.com.localpictureloader;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import java.util.Arrays;

import wzr.com.localpictureloader.core.picture.LoaderOptions;
import wzr.com.localpictureloader.core.picture.LocalPictureLoader;

public class MainActivity extends AppCompatActivity {
    private LocalPictureLoader pictureLoader;
    private TextView tvBitPath;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        pictureLoader = new LocalPictureLoader();
        tvBitPath = (TextView) findViewById(R.id.tv_bit_path);
    }

    public void tackFromCust(View view) {
        LoaderOptions options = new LoaderOptions();
        options.tackFrom = LocalPictureLoader.TACK_FROM_CUSTOMER_TACKER;
        pictureLoader.loadPicture(this, options, new LocalPictureLoader.PictureLoadCallback() {
            @Override
            public void onResult(LocalPictureLoader.Result result) {
                if(result.code == LocalPictureLoader.Result.CODE_SUCCESS){
                    tvBitPath.setText(Arrays.toString(result.bitmapPaths));
                }else{
                    tvBitPath.setText(result.msg);
                }
            }
        });
    }

    public void tackFromCamera(View view) {
        LoaderOptions options = new LoaderOptions();
        options.tackFrom = LocalPictureLoader.TACK_FROM_SYSTEM_CAMERA;
        pictureLoader.loadPicture(this, options, new LocalPictureLoader.PictureLoadCallback() {
            @Override
            public void onResult(LocalPictureLoader.Result result) {
                if(result.code == LocalPictureLoader.Result.CODE_SUCCESS){
                    tvBitPath.setText(Arrays.toString(result.bitmapPaths));
                }else{
                    tvBitPath.setText(result.msg);
                }
            }
        });
    }

    public void tackFromGallery(View view) {
        LoaderOptions options = new LoaderOptions();
        options.tackFrom = LocalPictureLoader.TACK_FROM_SYSTEM_GALLERY;
        pictureLoader.loadPicture(this, options, new LocalPictureLoader.PictureLoadCallback() {
            @Override
            public void onResult(LocalPictureLoader.Result result) {
                if(result.code == LocalPictureLoader.Result.CODE_SUCCESS){
                    tvBitPath.setText(Arrays.toString(result.bitmapPaths));
                }else{
                    tvBitPath.setText(result.msg);
                }
            }
        });
    }

    public void editPicture(View view) {
        LoaderOptions options = new LoaderOptions();
        options.tackFrom = LocalPictureLoader.TACK_FROM_SYSTEM_CAMERA;
        options.processForm = LocalPictureLoader.PROCESS_BITMAP_FROM_SYSTEM_EDITOR;
        pictureLoader.loadPicture(this, options, new LocalPictureLoader.PictureLoadCallback() {
            @Override
            public void onResult(LocalPictureLoader.Result result) {
                if(result.code == LocalPictureLoader.Result.CODE_SUCCESS){
                    tvBitPath.setText(Arrays.toString(result.bitmapPaths));
                }else{
                    tvBitPath.setText(result.msg);
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        pictureLoader.onResult(data,requestCode,resultCode);
    }
}
