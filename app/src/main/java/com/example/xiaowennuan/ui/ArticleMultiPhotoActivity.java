package com.example.xiaowennuan.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.example.xiaowennuan.R;
import com.example.xiaowennuan.db.ArticleHeartModel;
import com.example.xiaowennuan.db.ArticleModel;
import com.example.xiaowennuan.db.ArticlePhotoModel;
import com.example.xiaowennuan.util.ShareSDKHelper;

import org.litepal.crud.DataSupport;

import java.util.ArrayList;
import java.util.List;

public class ArticleMultiPhotoActivity extends AppCompatActivity {

    Toolbar toolbar;

    private int aId;

    private String category = "";

    private final static String TAG = "ArticleMultiActivity";

    private final static int HEART = 1;
    private final static int PHOTO = 2;
    private final static int MAIN = 0;

    private String title;
    private String desc;
    private String image1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_multi_photo);

        Intent intent = getIntent();
        aId = intent.getIntExtra("aid", -1);
        category = intent.getStringExtra("category");

        toolbar = (Toolbar) findViewById(R.id.article_multi_photo_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        //progressBar = (ProgressBar) findViewById(R.id.article_multi_photo_progressbar);
        //progressBar.setVisibility(View.VISIBLE);

        // 根据不同category初始化文章内容
        initArticle();
    }


    /**
     * 异步初始化handler
     */
    private Handler initHeartHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case HEART:
                    ArrayList<ArticleHeartModel> heartArrayList = (ArrayList<ArticleHeartModel>) msg.obj;
                    ArticleHeartModel heartItem = heartArrayList.get(0);
                    title = heartItem.title;
                    desc = heartItem.desc;
                    image1 = heartItem.image1;
                    toolbar.setTitle(heartItem.category);
                    final WebView heartWebView = (WebView) findViewById(R.id.article_multi_photo_content_web_view);
                    WebSettings heartSettings = heartWebView.getSettings();
                    heartSettings.setJavaScriptEnabled(true);
                    heartSettings.setDomStorageEnabled(true);
                    heartSettings.setAppCacheEnabled(true);
                    heartSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);

                    heartWebView.setWebViewClient(new WebViewClient() {

                        @Override
                        public void onPageFinished(WebView view, String url) {
                            //progressBar.setVisibility(View.GONE);
                        }
                    });
                    heartWebView.loadDataWithBaseURL("http", heartItem.content, "text/html", "utf-8", null);
                    break;
                case PHOTO:
                    ArrayList<ArticlePhotoModel> photoArrayList = (ArrayList<ArticlePhotoModel>) msg.obj;
                    ArticlePhotoModel photoItem = photoArrayList.get(0);
                    title = photoItem.title;
                    desc = photoItem.desc;
                    image1 = photoItem.image1;
                    toolbar.setTitle(photoItem.category);
                    WebView photoWebView = (WebView) findViewById(R.id.article_multi_photo_content_web_view);
                    WebSettings photoSettings = photoWebView.getSettings();
                    photoSettings.setJavaScriptEnabled(true);
                    photoSettings.setDomStorageEnabled(true);
                    photoSettings.setAppCacheEnabled(true);
                    photoSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
                    photoWebView.setWebViewClient(new WebViewClient() {
                        @Override
                        public void onPageFinished(WebView view, String url) {
                            //progressBar.setVisibility(View.GONE);
                        }

                    });
                    photoWebView.loadDataWithBaseURL("", photoItem.content, "text/html", "utf-8", null);
                    break;
                case MAIN:
                    ArrayList<ArticleModel> arrayList = (ArrayList<ArticleModel>) msg.obj;
                    ArticleModel item = arrayList.get(0);
                    title = item.title;
                    desc = item.desc;
                    image1 = item.image1;
                    toolbar.setTitle(item.category);
                    WebView webView = (WebView) findViewById(R.id.article_multi_photo_content_web_view);
                    WebSettings settings = webView.getSettings();
                    settings.setJavaScriptEnabled(true);
                    settings.setDomStorageEnabled(true);
                    settings.setAppCacheEnabled(true);
                    settings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
                    webView.setWebViewClient(new WebViewClient() {
                        @Override
                        public void onPageFinished(WebView view, String url) {
                            //progressBar.setVisibility(View.GONE);
                        }
                    });
                    webView.loadDataWithBaseURL("http", item.content, "text/html", "utf-8", null);
                    break;
                default:
                    Toast.makeText(ArticleMultiPhotoActivity.this, "文章不存在", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };


    /**
     * 初始化
     */
    private void initArticle() {

        new Thread(new Runnable() {
            @Override
            public void run() {
                Message message = new Message();
                Log.d(TAG, "分类：" + category);
                switch (category) {
                    case "heart":
                        List<ArticleHeartModel> heartList = DataSupport.where("aid = ?", String.valueOf(aId))
                                .find(ArticleHeartModel.class);
                        ArrayList<ArticleHeartModel> heartArrayList = (ArrayList<ArticleHeartModel>) heartList;

                        if (heartArrayList.size() == 1) {
                            message.what = HEART;
                            message.obj = heartArrayList;
                            initHeartHandler.sendMessage(message);
                        }
                        break;
                    case "photo":
                        List<ArticlePhotoModel> photoList = DataSupport.where("aid = ?", String.valueOf(aId))
                                .find(ArticlePhotoModel.class);
                        ArrayList<ArticlePhotoModel> photoArrayList = (ArrayList<ArticlePhotoModel>) photoList;
                        if (photoArrayList.size() == 1) {
                            message.what = PHOTO;
                            message.obj = photoArrayList;
                            initHeartHandler.sendMessage(message);
                        }
                        break;
                    default:
                        List<ArticleModel> list = DataSupport.where("aid = ?", String.valueOf(aId))
                                .find(ArticleModel.class);
                        ArrayList<ArticleModel> arrayList = (ArrayList<ArticleModel>) list;
                        if (arrayList.size() == 1) {
                            message.what = MAIN;
                            message.obj = arrayList;
                            initHeartHandler.sendMessage(message);
                        }
                        break;
                }
                //Log.d(TAG, "message.what: " + String.valueOf(message.what));

            }
        }).start();

        //Log.d(TAG, "aid:" + String.valueOf(aId));
        //Log.d(TAG, "size:" + String.valueOf(arrayList.size()));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.menu_article_toolbar_share:
                ArrayList<String> list = new ArrayList<>();
                String domain = this.getString(R.string.domain_name);
                String article_url = domain + "/articles/get_article/?id="
                        + String.valueOf(aId);
                list.add(title);  // setTitle
                list.add(article_url);  // setTitleUrl
                list.add(desc);  // setText
                if (image1 != "" || image1 != null) {
                    list.add(image1);  // setImageUrl
                } else {
                    list.add(domain + "/static/images/common/ic_launcher.png");  // 添加应用图标的url
                }
                list.add(article_url);  // setUrl
                list.add("my comment");  // setComment
                list.add("小温暖");  // setSite
                list.add(domain);  // setSiteUrl
                ShareSDKHelper helper = new ShareSDKHelper(this, list);
                helper.showShare();
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_article_toolbar, menu);
        return true;
    }

}
