package com.example.xiaowennuan.ui;

import android.annotation.SuppressLint;
import android.content.Context;
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
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.example.xiaowennuan.R;
import com.example.xiaowennuan.db.ArticleHeartModel;
import com.example.xiaowennuan.db.ArticleModel;
import com.example.xiaowennuan.db.ArticlePhotoModel;
import com.example.xiaowennuan.util.ShareSDKHelper;
import com.example.xiaowennuan.util.WebViewHelper;

import org.json.JSONArray;
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
    private String content;

    WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_multi_photo);
        //Log.d(TAG, "ArticleMultiActivity starting....");
        Intent intent = getIntent();
        aId = intent.getIntExtra("aid", -1);
        category = intent.getStringExtra("category");
        //Log.d(TAG, "category:" + category);
        toolbar = (Toolbar) findViewById(R.id.article_multi_photo_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        //progressBar = (ProgressBar) findViewById(R.id.article_multi_photo_progressbar);
        //progressBar.setVisibility(View.VISIBLE);
        webView = (WebView) findViewById(R.id.article_multi_photo_content_web_view);
        // 根据不同category初始化文章内容
        initArticle();
    }

    @Override
    protected void onDestroy() {
        webView.destroy();
        super.onDestroy();
    }

    /**
     * 异步初始化handler
     */
    private Handler initHeartHandler = new Handler() {
        public void handleMessage(Message msg) {
            System.out.println("msg.what:" + String.valueOf(msg.what));
            switch (msg.what) {
                case HEART:
                    ArrayList<ArticleHeartModel> heartArrayList = (ArrayList<ArticleHeartModel>) msg.obj;
                    ArticleHeartModel heartItem = heartArrayList.get(0);
                    title = heartItem.title;
                    desc = heartItem.desc;
                    image1 = heartItem.image1;
                    content = heartItem.content;
                    toolbar.setTitle(heartItem.category);
                    break;
                case PHOTO:
                    ArrayList<ArticlePhotoModel> photoArrayList = (ArrayList<ArticlePhotoModel>) msg.obj;
                    ArticlePhotoModel photoItem = photoArrayList.get(0);
                    title = photoItem.title;
                    desc = photoItem.desc;
                    image1 = photoItem.image1;
                    content = photoItem.content;
                    toolbar.setTitle(photoItem.category);
                    break;
                case MAIN:
                    ArrayList<ArticleModel> arrayList = (ArrayList<ArticleModel>) msg.obj;
                    ArticleModel item = arrayList.get(0);
                    title = item.title;
                    desc = item.desc;
                    image1 = item.image1;
                    content = item.content;
                    toolbar.setTitle(item.category);
                    break;
                default:
                    Toast.makeText(ArticleMultiPhotoActivity.this, "文章不存在", Toast.LENGTH_SHORT).show();
                    break;
            }

            WebViewHelper webViewHelper = new WebViewHelper(ArticleMultiPhotoActivity.this);
            webViewHelper.initWebView(webView, content);
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
                //Log.d(TAG, "分类：" + category);
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
