package com.example.xiaowennuan.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import org.litepal.crud.DataSupport;

import java.util.ArrayList;
import java.util.List;

import com.example.xiaowennuan.R;
import com.example.xiaowennuan.db.ArticleRandomModel;

public class RandomMultiPhotoActivity extends AppCompatActivity {

    //ProgressBar progressBar;

    private final static String TAG = "RArticleMultiActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_multi_photo);

        Toolbar toolbar = (Toolbar) findViewById(R.id.article_multi_photo_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        //progressBar = (ProgressBar) findViewById(R.id.article_multi_photo_progressbar);

        initArticle();
    }


    /**
     * 异步初始化handler
     */
    private Handler initHeartHandler = new Handler() {
        public void handleMessage(Message msg) {

            ArticleRandomModel item = (ArticleRandomModel) msg.obj;
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
            List<ArticleRandomModel> photoList = DataSupport.findAll(ArticleRandomModel.class);
            ArrayList<ArticleRandomModel> arrayList = (ArrayList<ArticleRandomModel>) photoList;
            if (arrayList.size() == 1) {
                message.obj = arrayList.get(0);
                initHeartHandler.sendMessage(message);
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
