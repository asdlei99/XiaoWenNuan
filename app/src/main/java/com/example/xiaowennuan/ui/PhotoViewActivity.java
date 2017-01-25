package com.example.xiaowennuan.ui;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.FutureTarget;
import com.example.xiaowennuan.R;
import com.example.xiaowennuan.util.SingleMediaScanner;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

import uk.co.senab.photoview.PhotoView;

public class PhotoViewActivity extends AppCompatActivity {

    private ViewPager viewPager;

    private Toolbar toolbar;

    TextView pageIndicator;

    private List<String> imgs;

    private int position;
    private int currentPosition;

    private PagerAdapter pagerAdapter;

    private String currentImageUrl;
    private String currentCachedImageUrl;

    private boolean isDownloading = false;

    private MediaScannerConnection conn;

    //ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_view);

        toolbar = (Toolbar) findViewById(R.id.photo_view_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }


        pageIndicator = (TextView) findViewById(R.id.photo_view_toolbar_title);

        //progressBar = (ProgressBar) findViewById(R.id.photo_view_progressbar);

        position = getIntent().getIntExtra("position", 0);  // activity初始position
        imgs = getIntent().getStringArrayListExtra("imgs");
        currentImageUrl = imgs.get(position);  // 初始化时的图片url

        viewPager = (ViewPager) this.findViewById(R.id.imgs_viewpager);
        viewPager.setOffscreenPageLimit(2);

        System.out.println("imgs:" + imgs.toString());
        System.out.println("position:" + position);
        System.out.println("imgs :" + imgs.size());

        pagerAdapter = new MyViewPagerAdapter(this, imgs, position);
        viewPager.setAdapter(pagerAdapter);
        viewPager.setCurrentItem(position);


        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                currentPosition = position;
                System.out.println("onPageSelected:" + position);
                pageIndicator.setText((currentPosition + 1) + "/" + (imgs.size()));
                currentImageUrl = imgs.get(currentPosition);  // 拿到当前位置图片的url
                // 每次切换viewpager，都初始化
                downloadInit();
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        pageIndicator.setText((position + 1) + "/" + (imgs.size()));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_photo_view_toolbar, menu);
        downloadInit();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    /**
     * 初始化Toolbar的下载menu
     */
    private void downloadInit() {
        toolbar.getMenu().findItem(R.id.photo_view_toolbar_down).setOnMenuItemClickListener(
            new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    if (!isDownloading) {
                        isDownloading = true;
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                final File savedImage = saveImage();  // 保存图片
                                //updateGallery(savedPath);  // 更新图库
                                new SingleMediaScanner(PhotoViewActivity.this, savedImage);
                                // UI线程中通知用户结果
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (savedImage!=null) {
                                            Toast.makeText(PhotoViewActivity.this,
                                                    String.format("图片已保存到%s", savedImage.getAbsolutePath()),
                                                    Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(PhotoViewActivity.this,
                                                    "图片保存失败，请重试", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                                // 图片保存完成后状态置为false
                                isDownloading = false;
                            }
                        }).start();
                    }
                    return true;
                }
            });
    }

    /**
     * 获取缓存的图片路径
     * @param url 目标缓存图片路径
     * @return
     */
    private String getCachedImagePath(String url) {
        FutureTarget<File> futureTarget = Glide.with(PhotoViewActivity.this)
                .load(url).downloadOnly(100,100);
        try {
            File cacheImage = futureTarget.get();
            String path = cacheImage.getAbsolutePath();
            return path;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * copy缓存图片到指定目录，并通知图库刷新
     */
    private File saveImage() {

        // 获取当前位置图片的缓存路径
        String cachedImage = getCachedImagePath(currentImageUrl);

        // 新路径
        File newPath = new File(Environment.getExternalStorageDirectory(), "Xiaowennuan/download/images/xiaowennuan");
        if(newPath.exists() && newPath.isFile()) {
            newPath.delete();
        }
        if (!newPath.exists()) {
            newPath.mkdirs();
        }
        System.out.println("路径是否存在？" + newPath.exists());
        System.out.println(cachedImage);
        // copying
        try {
            int byteRead;
            File cachedFile = new File(cachedImage);
            if (cachedFile.exists()) {
                String newName = String.format("IMG_%s%s", getCurrentDateString(),
                        getImageSuffix(currentImageUrl));
                File newImage = new File(newPath, newName);
                InputStream inStream = new FileInputStream(cachedImage);
                FileOutputStream fs = new FileOutputStream(newImage);
                byte[] buffer = new byte[1024];
                while ((byteRead = inStream.read(buffer)) != -1) {
                    fs.write(buffer, 0, byteRead);
                }
                inStream.close();
                return newImage;
            }
        } catch (Exception e) {
            System.out.println("复制文件操作出错");
            e.printStackTrace();
        }
        return null;
    }

    /**
     *
     * //@param newFile 需更新的单个文件
     */
    /*private void updateGallery(String newFile) {
        MediaScannerConnection.scanFile(this,
            new String[] { newFile }, null,
            new MediaScannerConnection.OnScanCompletedListener() {
                public void onScanCompleted(String path, Uri uri) {
                    Log.i("ExternalStorage", "Scanned " + path + ":");
                    Log.i("ExternalStorage", "-> uri=" + uri);
                }
            });
    }*/



    private String getImageSuffix(String imageUrl) {
        String suffix = imageUrl.substring(imageUrl.lastIndexOf("."), imageUrl.length());
        return suffix;
    }

    private String getCurrentDateString() {
        Date date = new Date();
        DateFormat format = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String datetime = format.format(date);
        return datetime;
    }


    public class MyViewPagerAdapter extends PagerAdapter {

        List<String> imgs;

        List<View> views;

        Context mContext;

        int mPosition; //这个是当前position

        ProgressBar progressBar;

        public MyViewPagerAdapter(Context context, List<String> list, int position) {

            this.mContext = context;
            this.imgs = list;

            this.views = new ArrayList<>();

            this.mPosition = position;
            //this.progressBar = progressBar;

        }

        @Override
        public int getCount() { // 获得size
            return imgs.size();
        }

        @Override
        public boolean isViewFromObject(View arg0, Object arg1) {
            return arg0 == arg1;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {

            container.removeView((PhotoView) object);  //删除页卡
        }


        @Override
        public Object instantiateItem(ViewGroup container, int position) {

            System.out.println("currentPosition:"+currentPosition);

            //GestureImageView full_image = new GestureImageView(mContext);
            PhotoView photoView = new PhotoView(mContext);

            //full_image
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
            photoView.setLayoutParams(params);
            photoView.setScaleType(ImageView.ScaleType.FIT_CENTER);


            //progressBar.setVisibility(View.VISIBLE);
            System.out.println("position:"+position+"------->>>>  url "+imgs.get(position));

            // SOURCE为保存原图
            Glide.with(mContext)
                    .load(imgs.get(position))
                    .into(photoView);

            container.addView(photoView);

            return photoView;
        }
    }

}



