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

public class RandomArticleActivity extends AppCompatActivity {

    CollapsingToolbarLayout collapsingToolbar;

    //ProgressBar progressBar;

    private final static String TAG = "RandomArticleActivity";

    ImageView toolBarImageView;

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

            ArticleRandomModel item = (ArticleRandomModel) msg.obj;
            if (item.image1 != null) {
                Glide.with(RandomArticleActivity.this).load(item.image1)
                        .placeholder(R.drawable.placeholder_big).into(toolBarImageView);
            }
            // 设置toolbar标题
            collapsingToolbar.setTitle(item.category);

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
            webView.loadDataWithBaseURL("http", item.content, "text/html", "utf-8", null);
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
        }
        return super.onOptionsItemSelected(menuItem);
    }


}
