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
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.bumptech.glide.Glide;
import com.example.xiaowennuan.db.ArticleRandomModel;

import org.litepal.crud.DataSupport;

import java.util.ArrayList;
import java.util.List;

import com.example.xiaowennuan.R;
import com.example.xiaowennuan.util.ShareSDKHelper;

public class RandomArticleActivity extends AppCompatActivity {

    CollapsingToolbarLayout collapsingToolbar;

    //ProgressBar progressBar;

    private final static String TAG = "RandomArticleActivity";

    private ArticleRandomModel model;

    private ImageView toolBarImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article);

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

        initArticleContent();

    }


    /**
     * 异步初始化
     */
    private Handler initMainHandler = new Handler() {
        public void handleMessage(Message msg) {

            model = (ArticleRandomModel) msg.obj;
            if (model.image1 != null) {
                Glide.with(RandomArticleActivity.this).load(model.image1)
                        .placeholder(R.drawable.placeholder_big).into(toolBarImageView);
            }
            // 设置toolbar标题
            collapsingToolbar.setTitle(model.category);

            WebView webView = (WebView) findViewById(R.id.article_content_web_view);
            WebSettings settings = webView.getSettings();
            settings.setJavaScriptEnabled(true);
            settings.setDomStorageEnabled(true);
            //settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
            settings.setAppCacheEnabled(true);
            settings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
            webView.setWebViewClient(new WebViewClient() {
                // webview加载完成
                @Override
                public void onPageFinished(WebView view, String url) {
                    //progressBar.setVisibility(View.GONE);
                }
            });
            webView.loadDataWithBaseURL("http", model.content, "text/html", "utf-8", null);
        }
    };

    private void initArticleContent() {

        new Thread(new Runnable() {
            @Override
            public void run() {

            List<ArticleRandomModel> list = DataSupport.findAll(ArticleRandomModel.class);
            ArrayList<ArticleRandomModel> arrayList = (ArrayList<ArticleRandomModel>) list;
            Message message = new Message();
            if (arrayList.size() == 1) {
                message.obj = arrayList.get(0);
                initMainHandler.sendMessage(message);
            }
            DataSupport.deleteAll(ArticleRandomModel.class);
            }
        }).start();
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
