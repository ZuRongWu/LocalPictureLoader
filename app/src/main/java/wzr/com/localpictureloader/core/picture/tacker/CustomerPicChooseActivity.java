package wzr.com.localpictureloader.core.picture.tacker;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.concurrent.locks.ReentrantLock;

import wzr.com.localpictureloader.R;
import wzr.com.localpictureloader.util.EventLog;
import wzr.com.localpictureloader.util.FileUtil;

/**
 * 图片获取activity
 * Created by wuzr on 2016/9/30.
 */
public class CustomerPicChooseActivity extends Activity{
    public static final String RESLT_PATH_ARRAY = "result_path_array";
    private LocalPictureInfo pictureInfo;
    private H handler;
    private TextView tvComplete;
    private BaseAdapter imageAdapter;
    private GridView gvImage;
    //选择的图片的绝对路径
    private ArrayList<String> picPathChoosed = new ArrayList<>();
    //当前需要在gridview中显示的相册
    private BucketInfo currentBucket;
    private BucketListWindow bucketListWindow;
    //作为bucketListWindow的锚点
    private TextView tvBucket;
    //用于从文件中加载图片
    private ImageDisplayer displayer;

    private static class H extends Handler{
        static final int SCAN_COMPLETED = 1;
        private CustomerPicChooseActivity host;
        H(CustomerPicChooseActivity host){
            this.host = host;
        }

        @Override
        public void handleMessage(Message msg) {
            if(msg.what == SCAN_COMPLETED){
                host.onScanLocalPicComplete();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EventLog.addMarker("load_picture", "start_chooser_activity");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_chooser);
        handler = new H(this);
        initData();
        initView();
    }

    private void initView(){
        findViewById(R.id.tv_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent result = new Intent();
                result.putExtra(RESLT_PATH_ARRAY, new String[0]);
                setResult(RESULT_OK, result);
                EventLog.addMarker("load_picture","choose_cancel");
                finish();
            }
        });
        tvComplete = (TextView) findViewById(R.id.tv_complete);
        tvComplete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //设置结果，结束activity
                Intent result = new Intent();
                String[] picPaths = new String[picPathChoosed.size()];
                for (int i = 0; i < picPathChoosed.size(); i++) {
                    picPaths[i] = picPathChoosed.get(i);
                }
                result.putExtra(RESLT_PATH_ARRAY, picPaths);
                setResult(RESULT_OK, result);
                EventLog.addMarker("load_picture", "choose_complete");
                finish();
            }
        });
        tvBucket = (TextView) findViewById(R.id.tv_all);
        tvBucket.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //显示相册列表
                showBucketList();
            }
        });
        gvImage = (GridView) findViewById(R.id.gv_image);
        imageAdapter = new GridImageAdapter();
        gvImage.setAdapter(imageAdapter);
    }

    private void initData(){
        displayer = new ImageDisplayer();
        new Thread(){
            @Override
            public void run() {
                scanLocalPicture();
                Message msg = handler.obtainMessage(H.SCAN_COMPLETED);
                handler.sendMessage(msg);
            }
        }.start();
    }

    private void scanLocalPicture(){
        ContentResolver cr = getContentResolver();
        //1.获取所有外部存储的图片的缩略图
        HashMap<String,String> thumbPathsById = new HashMap<>();
        String[] thumbColumns = {MediaStore.Images.Thumbnails.IMAGE_ID,
                MediaStore.Images.Thumbnails.DATA };
        Cursor thumbCur = cr.query(MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI, thumbColumns,
                null, null, null);
        if(thumbCur != null&&thumbCur.moveToFirst()){
            int idIndex = thumbCur.getColumnIndex(MediaStore.Images.Thumbnails.IMAGE_ID);
            int dataIndex = thumbCur.getColumnIndex(MediaStore.Images.Thumbnails.DATA);
            do {
                thumbPathsById.put(String.valueOf(thumbCur.getInt(idIndex)),thumbCur.getString(dataIndex));
            }while (thumbCur.moveToNext());
            thumbCur.close();
        }
        //2.获取所有保存在外部存储的图片
        if(pictureInfo == null){
            pictureInfo = new LocalPictureInfo();
        }
        // 构造相册索引
        String[] imgColumn = new String[] { MediaStore.Images.Media._ID, MediaStore.Images.Media.BUCKET_ID,
                 MediaStore.Images.Media.DATA, MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_ADDED, MediaStore.Images.Media.BUCKET_DISPLAY_NAME };
        // 得到一个游标
        Cursor imageCur = cr.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imgColumn, null, null,
                null);
        if(imageCur != null&&imageCur.moveToFirst()){
            int imgIdIndex = imageCur.getColumnIndex(MediaStore.Images.Media._ID);
            int bucketIdIndex = imageCur.getColumnIndex(MediaStore.Images.Media.BUCKET_ID);
            int dataIndex = imageCur.getColumnIndex(MediaStore.Images.Media.DATA);
            int imgNameIndex = imageCur.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME);
            int dateIndex = imageCur.getColumnIndex(MediaStore.Images.Media.DATE_ADDED);
            int bucketNameIndex = imageCur.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME);
            do{
                File img = new File(imageCur.getString(dataIndex));
                if(img.exists()){
                    ImageInfo imageInfo = new ImageInfo();
                    imageInfo.contentPath = img.getAbsolutePath();
                    imageInfo.dateFormMillisecond = imageCur.getLong(dateIndex);
                    imageInfo.imageName = imageCur.getString(imgNameIndex);
                    String imgId = String.valueOf(imageCur.getInt(imgIdIndex));
                    imageInfo.thumbPath = thumbPathsById.get(imgId);
                    File thumb = imageInfo.thumbPath == null?null:new File(imageInfo.thumbPath);
                    if(thumb == null||!thumb.exists()){
                        imageInfo.thumbPath = imageInfo.contentPath;
                    }
                    String bucketId = imageCur.getString(bucketIdIndex);
                    BucketInfo bucketInfo = pictureInfo.imageBucketById.get(bucketId);
                    if(bucketInfo == null){
                        bucketInfo = new BucketInfo();
                        pictureInfo.imageBucketById.put(bucketId,bucketInfo);
                        pictureInfo.imageBucketList.add(bucketInfo);
                    }
                    bucketInfo.bucketId = bucketId;
                    bucketInfo.bucketName = imageCur.getString(bucketNameIndex);
                    if(bucketInfo.imageInfos == null||bucketInfo.imageInfos.size() == 0){
                        bucketInfo.imageInfos = new ArrayList<>();
                        bucketInfo.coverPath = imageInfo.thumbPath;
                    }
                    bucketInfo.imageInfos.add(imageInfo);
                }
            }while (imageCur.moveToNext());
            imageCur.close();
        }
    }

    private void onScanLocalPicComplete(){
        currentBucket = pictureInfo.imageBucketList.get(0);
        //隐藏进度条
        findViewById(R.id.ll_loading).setVisibility(View.GONE);
        imageAdapter.notifyDataSetChanged();
    }

    private void showBucketList(){
        if(bucketListWindow == null){
            bucketListWindow = new BucketListWindow();
        }
        bucketListWindow.showAsDropDown(tvBucket);
    }

    private void onBucketChange(BucketInfo bucketInfo){
        currentBucket = bucketInfo;
        imageAdapter.notifyDataSetChanged();
    }

    @Override
    public void onBackPressed() {
        if(bucketListWindow != null&&bucketListWindow.isShowing()){
            bucketListWindow.dismiss();
        }else{
            Intent result = new Intent();
            result.putExtra(RESLT_PATH_ARRAY, new String[0]);
            setResult(RESULT_OK, result);
            EventLog.addMarker("load_picture","choose_cancel");
            super.onBackPressed();
        }
    }

    private static class LocalPictureInfo{
        public HashMap<String,BucketInfo> imageBucketById = new HashMap<>();
        public ArrayList<BucketInfo> imageBucketList = new ArrayList<>();
    }

    private static class BucketInfo{
        public String bucketId;
        public String bucketName;
        //封面图片的路径，为此文件夹下的第一张图片的缩略图
        public String coverPath;
        public int defaultCoverRes = R.mipmap.ic_no_photo;
        public ArrayList<ImageInfo> imageInfos;
    }

    private static class ImageInfo{
        public String imageName;
        public long dateFormMillisecond;
        //YY-MM-DD HH:mm:ss
        public String dateFormEasy;
        //图片路径
        public String contentPath;
        //缩略图路径
        public String thumbPath;
        public int defaultThumbRes = R.mipmap.ic_no_photo;
    }

    private class GridImageAdapter extends BaseAdapter{

        @Override
        public int getCount() {
            return currentBucket == null?0:currentBucket.imageInfos.size();
        }

        @Override
        public Object getItem(int i) {
            return currentBucket == null?0:currentBucket.imageInfos.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(final int i, View view, ViewGroup viewGroup) {
             GridImageHolder holder;
            int w,h;
            if(view == null){
                view = View.inflate(CustomerPicChooseActivity.this,R.layout.layout_item_gride_image,null);
                holder = new GridImageHolder();
                holder.ivThumb = (ImageView) view.findViewById(R.id.iv_thumb);
                holder.cbCheck = (CheckBox) view.findViewById(R.id.cb_check);
                holder.vmShade = view.findViewById(R.id.vw_shade);
                w = h = (gvImage.getWidth() - gvImage.getPaddingLeft() - gvImage.getPaddingRight())/3;
                RelativeLayout.LayoutParams rl1 = new RelativeLayout.LayoutParams(w,h);
                holder.ivThumb.setLayoutParams(rl1);
                RelativeLayout.LayoutParams rl2 = new RelativeLayout.LayoutParams(w,h);
                holder.vmShade.setLayoutParams(rl2);
                view.setTag(holder);
            }else{
                holder = (GridImageHolder) view.getTag();
                w = h = holder.ivThumb.getWidth();
            }
            String thumbPath = currentBucket.imageInfos.get(i).thumbPath;
            if(thumbPath == null||thumbPath.equals("")){
                holder.ivThumb.setImageResource(currentBucket.imageInfos.get(i).defaultThumbRes);
            }else{
                File f = new File(thumbPath);
                if(!f.exists()){
                    holder.ivThumb.setImageResource(currentBucket.imageInfos.get(i).defaultThumbRes);
                }else{
//                    //限制图片的大小不超过ivThumb的宽高
//                    holder.ivThumb.setImageBitmap(FileUtil.readImageFromFile(f,null,w/2,h/2));
                    displayer.display(f,holder.ivThumb,60,60);
                }
            }
            String contentPath = currentBucket.imageInfos.get(i).contentPath;
            if(picPathChoosed.contains(contentPath)){
                holder.cbCheck.setChecked(true);
                holder.ivThumb.setColorFilter(Color.parseColor("#77000000"));
            }else{
                holder.cbCheck.setChecked(false);
                holder.ivThumb.setColorFilter(Color.parseColor("#00000000"));
            }
            final GridImageHolder fHolder = holder;
            holder.cbCheck.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String contentPath = currentBucket.imageInfos.get(i).contentPath;
                    if(picPathChoosed.contains(contentPath)){
                        //移除选择
                        picPathChoosed.remove(contentPath);
                        fHolder.cbCheck.setChecked(false);
                        fHolder.ivThumb.setColorFilter(Color.parseColor("#00000000"));
                    }else{
                        //添加选择
                        picPathChoosed.add(contentPath);
                        fHolder.cbCheck.setChecked(true);
                        fHolder.ivThumb.setColorFilter(Color.parseColor("#77000000"));
                    }
                    String msg = picPathChoosed.size() > 0?"完成("+ picPathChoosed.size() + ")":"完成";
                    tvComplete.setText(msg);
                }
            });
            return view;
        }

        private class GridImageHolder{
            ImageView ivThumb;
            View vmShade;
            CheckBox cbCheck;
        }
    }

    /**
     * 相册列表window
     */
    private class BucketListWindow extends PopupWindow{
        BucketListWindow(){
            super();
            setAnimationStyle(R.style.bucket_list_window_anim);
            DisplayMetrics dm = getResources().getDisplayMetrics();
            setHeight((int) (dm.heightPixels * 0.7f));
            setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
            setFocusable(true);
            setOutsideTouchable(true);
            setBackgroundDrawable(new ColorDrawable(Color.parseColor("#70000000")));
            View contentView = View.inflate(CustomerPicChooseActivity.this,R.layout.layout_bucket_window_list,null);
            setContentView(contentView);
            ListView lvBucketList = (ListView) contentView;
            lvBucketList.setAdapter(new BucketAdapter());
            lvBucketList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    onBucketChange(pictureInfo.imageBucketList.get(i));
                    dismiss();
                }
            });
        }
        private class BucketAdapter extends BaseAdapter{

            @Override
            public int getCount() {
                return pictureInfo.imageBucketList != null?pictureInfo.imageBucketList.size():0;
            }

            @Override
            public Object getItem(int i) {
                return pictureInfo.imageBucketList != null?pictureInfo.imageBucketList.get(i):null;
            }

            @Override
            public long getItemId(int i) {
                return i;
            }

            @Override
            public View getView(int i, View view, ViewGroup viewGroup) {
                BucketHolder holder;
                if(view == null){
                    view = View.inflate(CustomerPicChooseActivity.this,R.layout.layout_bucket_list_item,null);
                    holder = new BucketHolder();
                    holder.ivCover = (ImageView) view.findViewById(R.id.iv_cover);
                    holder.tvName = (TextView) view.findViewById(R.id.tv_name);
                    holder.tvNum = (TextView) view.findViewById(R.id.tv_num);
                    view.setTag(holder);
                }else{
                    holder = (BucketHolder) view.getTag();
                }
                BucketInfo bucketInfo = pictureInfo.imageBucketList.get(i);
                if(bucketInfo.coverPath == null||bucketInfo.coverPath.equals("")){
                    holder.ivCover.setImageResource(R.mipmap.ic_no_photo);
                }else{
                    File f = new File(bucketInfo.coverPath);
                    if(f.exists()){
//                        holder.ivCover.setImageBitmap(FileUtil.readImageFromFile(f,null,90,90));
                        displayer.display(f,holder.ivCover,45,45);
                    }else{
                        holder.ivCover.setImageResource(R.mipmap.ic_no_photo);
                    }
                }
                holder.tvName.setText(bucketInfo.bucketName);
                holder.tvNum.setText(String.valueOf(bucketInfo.imageInfos.size()));
                return view;
            }

            private class BucketHolder{
                ImageView ivCover;
                TextView tvName;
                TextView tvNum;
            }
        }
    }

    public static class ImageDisplayer{
        public static final int CACHE_MAX_LENGTH = 40;
        public static final int MAX_THREAD = 4;
        private HashMap<String,SoftReference<Bitmap>> cache = new HashMap<>(20,0.5f);
        private ArrayList<String> cacheKeys = new ArrayList<>(20);
        private HashMap<ImageView,String> displayViewByKey = new HashMap<>(6);
        private ArrayList<DisplayRequest> waitLoadQueue = new ArrayList<>(8);
        private Hashtable<ImageView,DisplayRequest> waitShowQueue = new Hashtable<>(6,0.5f);
        private int currentThreadNum;
        private ReentrantLock lock;

        public void display(File f,ImageView view,int targetWidth,int targetHeight){
            if(lock == null){
                lock = new ReentrantLock();
            }
            //检查有没有等待显示图片的imageView，如果有，从waitShowQueue中移除
            if(waitShowQueue.containsKey(view)){
                waitShowQueue.remove(view);
            }
            displayViewByKey.put(view,f.getAbsolutePath());
            //从缓存中查找
            SoftReference<Bitmap> sb = cache.get(f.getAbsolutePath());
            Bitmap bitmap = sb != null?sb.get():null;
            if(bitmap != null&&!bitmap.isRecycled()){
                Log.d("debug","命中缓存");
                view.setImageBitmap(bitmap);
                return;
            }
            /*
            * 把请求放到等待队列中，如果当前线程数没有达到最大值，则开启新线程加载图片
            * */
            DisplayRequest r = new DisplayRequest();
            r.file = f;
            r.imageView = view;
            r.targetHeight = targetHeight;
            r.targetWidth = targetWidth;
            lock.lock();
            waitLoadQueue.add(r);
            lock.unlock();
            r.imageView.setImageResource(R.mipmap.ic_no_photo);
            if(currentThreadNum >= MAX_THREAD){
                return;
            }
            //创建新的工作线程
            currentThreadNum++;
            new DisplayTask(this).start();
        }

        private DisplayRequest getRequest(){
            DisplayRequest request = null;
            lock.lock();
            if(waitLoadQueue.size() > 0){
                request = waitLoadQueue.remove(0);
            }
            lock.unlock();
            return request;
        }

        private void addCache(String key,Bitmap bm){
            //当free>=targetFree时添加缓存
            if(clearCache()){
                cache.put(key,new SoftReference<>(bm));
                cacheKeys.add(key);
            }
            Log.d("debug", "cache_size = " + cache.size());
        }

        private boolean clearCache(){
            //先清理旧缓存,使空闲内存至少占总内存的8/1
            long targetFree = Runtime.getRuntime().totalMemory()/8;
            long free = Runtime.getRuntime().freeMemory();
            while(free < targetFree){
                if(cache.size() > 0&&cacheKeys.size() > 0){
                    String k = cacheKeys.remove(0);
                    SoftReference<Bitmap> sb = cache.remove(k);
                    Bitmap old = sb != null?sb.get():null;
                    free += old != null?old.getByteCount():0;
                }else{
                    Log.w("debug","内存不够了");
                    break;
                }
            }
            return free > targetFree;
        }

        /**
         * @return 当要将要显示的图片的路径与实际要显示的一致时返回true
         * 由于加载图片放在其他线程中，存在这种情况：加载图片线程在加载图片
         * 时调用端让显示图片的imageView显示其他图片了，这时就不应该在显示本次
         * 加载的图片了，返回false
         */
        private boolean shouldDisplay(DisplayRequest request){
            return request.file.getAbsolutePath().equals(displayViewByKey.get(request.imageView));
        }

        private class DisplayRequest{
            File file;
            ImageView imageView;
            int targetWidth;
            int targetHeight;
            Bitmap result;
        }

        private static class DisplayTask extends Thread{

             static class DisplayHandler extends Handler{
                 static final int SHOW_IMAGE = 1;
                 private boolean isWaitDisplay;
                 private ImageDisplayer displayer;
                 private Runnable showImag = new Runnable() {
                     @Override
                     public void run() {
                         isWaitDisplay = false;
                         for (ImageView key : displayer.waitShowQueue.keySet()) {
                             DisplayRequest request = displayer.waitShowQueue.get(key);
                             if(request != null){
                                 request.imageView.setImageBitmap(request.result);
                             }
                         }
                         displayer.waitShowQueue.clear();
                     }
                 };
                 DisplayHandler(ImageDisplayer displayer){
                     this.displayer = displayer;
                 }
                @Override
                public void handleMessage(Message msg) {
                    if(msg.what == SHOW_IMAGE){
                        DisplayRequest request = (DisplayRequest) msg.obj;
                        if(request == null||!displayer.shouldDisplay(request)){
                            if (request != null){
                                /*
                                * 这个为什么会为空呢？想不明白
                                * */
                                if(request.result != null){
                                    request.result.recycle();
                                }
                            }
                            return;
                        }
                        //添加到缓存
                        if(displayer.cache.size() < CACHE_MAX_LENGTH){
                            displayer.addCache(request.file.getAbsolutePath(),request.result);
                        }else{
                            if(displayer.cache.size() > 0||displayer.cacheKeys.size() > 0){
                                String key = displayer.cacheKeys.remove(0);
                                displayer.cache.remove(key);
                                displayer.addCache(request.file.getAbsolutePath(),request.result);
                            }
                        }
                        displayer.waitShowQueue.put(request.imageView,request);
                        if(!isWaitDisplay){
                            isWaitDisplay = true;
                            this.postDelayed(showImag, 800);
                        }
                    }
                }
            }
            private ImageDisplayer displayer;
            private Handler handler;
            private DisplayTask(ImageDisplayer displayer){
                this.displayer = displayer;
                handler = new DisplayHandler(displayer);
            }
            @Override
            public void run() {
                DisplayRequest request;
                while ((request = displayer.getRequest()) != null){
                    if(!displayer.shouldDisplay(request)){
                        continue;
                    }
                    Bitmap bitmap;
                    try{
                        displayer.clearCache();
                        bitmap = FileUtil.readImageFromFile(request.file, null, request.targetWidth, request.targetHeight);
                    }catch (OutOfMemoryError error){
                        error.printStackTrace();
                        displayer.cacheKeys.clear();
                        displayer.cache.clear();
                        bitmap = FileUtil.readImageFromFile(request.file,null,request.targetWidth/2,request.targetHeight/2);
                        Log.w("debug","糟糕的情况");
                    }
                    request.result = bitmap;
                    Message message = handler.obtainMessage(DisplayHandler.SHOW_IMAGE);
                    message.obj = request;
                    handler.sendMessage(message);
                }
                displayer.currentThreadNum --;
            }
        }
    }
}
