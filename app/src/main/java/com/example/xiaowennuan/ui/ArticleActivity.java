package com.example.xiaowennuan.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.xiaowennuan.R;
import com.example.xiaowennuan.db.ArticleModel;
import com.example.xiaowennuan.db.ArticleReadModel;
import com.example.xiaowennuan.util.ShareSDKHelper;
import com.example.xiaowennuan.util.WebViewHelper;

import org.litepal.crud.DataSupport;

import java.util.ArrayList;
import java.util.List;

public class ArticleActivity extends AppCompatActivity {

    CollapsingToolbarLayout collapsingToolbar;

    //ProgressBar progressBar;

    private final static String TAG = "ArticleActivity";

    private String category;

    private int aId;

    ImageView toolBarImageView;

    private WebView webView;

    private final static int READ = 1;
    private final static int MAIN = 0;

    ArticleModel model;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article);

        Intent intent = getIntent();
        aId = intent.getIntExtra("aid", -1);
        category = intent.getStringExtra("category");

        Toolbar toolbar = (Toolbar) findViewById(R.id.article_toolbar);
        collapsingToolbar = (CollapsingToolbarLayout)
                findViewById(R.id.article_collapsing_toolbar_layout);
        toolBarImageView = (ImageView) findViewById(R.id.article_toolbar_image_view);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        //progressBar = (ProgressBar) findViewById(R.id.article_progressbar);
        webView = (WebView) findViewById(R.id.article_content_web_view);
        initArticleContent();

    }

    @Override
    protected void onDestroy() {
        webView.destroy();
        super.onDestroy();
    }

    /**
     * 异步初始化
     */
    private Handler initReadHandler = new Handler() {
      public void handleMessage(Message msg) {
          switch (msg.what) {
              case READ:
                  ArrayList<ArticleReadModel> readArrayList = (ArrayList<ArticleReadModel>) msg.obj;
                  ArticleReadModel item = readArrayList.get(0);
                  if (item.image1 != null) {
                      Glide.with(ArticleActivity.this).load(item.image1)
                              .placeholder(R.drawable.placeholder_big).into(toolBarImageView);
                  }
                  // 设置toolbar标题
                  collapsingToolbar.setTitle(item.category);

                  // 初始化WebView
                  WebViewHelper helper = new WebViewHelper(ArticleActivity.this);
                  helper.initWebView(webView, item.content);
                  break;
              default:
                  Toast.makeText(ArticleActivity.this, "文章不存在", Toast.LENGTH_SHORT).show();
          }
      }
    };
    private Handler initMainHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MAIN:
                    ArrayList<ArticleModel> arrayList = (ArrayList<ArticleModel>) msg.obj;
                    model = arrayList.get(0);
                    if (model.image1 != null) {
                        Log.d(TAG, model.image1);
                        Glide.with(ArticleActivity.this).load(model.image1)
                                .placeholder(R.drawable.placeholder_big).into(toolBarImageView);
                    }
                    // 设置toolbar标题
                    collapsingToolbar.setTitle(model.category);

                    final WebView webView = (WebView) findViewById(R.id.article_content_web_view);
                    WebViewHelper helper = new WebViewHelper(ArticleActivity.this);
                    helper.initWebView(webView, model.content);
            }
        }
    };

    private void initArticleContent() {

        new Thread(new Runnable() {
            @Override
            public void run() {
                switch (category) {
                    case "read":
                        List<ArticleReadModel> readList = DataSupport.where("aid = ?", String.valueOf(aId))
                                .find(ArticleReadModel.class);
                        ArrayList<ArticleReadModel> readArrayList = (ArrayList<ArticleReadModel>) readList;
                        Message messageRead = new Message();
                        if (readArrayList.size() == 1) {
                            messageRead.what = READ;
                            messageRead.obj = readArrayList;
                            initReadHandler.sendMessage(messageRead);
                        }
                        break;
                    default:
                        List<ArticleModel> list = DataSupport.where("aid = ?", String.valueOf(aId))
                                .find(ArticleModel.class);
                        ArrayList<ArticleModel> arrayList = (ArrayList<ArticleModel>) list;
                        Message message = new Message();
                        if (arrayList.size() == 1) {
                            message.what = MAIN;
                            message.obj = arrayList;
                            initMainHandler.sendMessage(message);
                        }
                }

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
                        + String.valueOf(model.aId);
                list.add(model.title);  // setTitle
                list.add(article_url);  // setTitleUrl
                list.add(model.desc);  // setText
                if (model.image1 != "" || model.image1 != null) {
                    list.add(model.image1);  // setImageUrl
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
