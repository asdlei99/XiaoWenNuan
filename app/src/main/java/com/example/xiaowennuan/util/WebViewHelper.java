package com.example.xiaowennuan.util;

import android.content.Context;
import android.content.Intent;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.example.xiaowennuan.ui.PhotoViewActivity;

import java.util.ArrayList;

/**
 * Created by Oliver on 2017/1/23.
 */

public class WebViewHelper {

    private Context mContext;

    public WebViewHelper(Context context) {
        mContext = context;
    }

    /**
     * 初始化Webview
     */
    public void initWebView(final WebView webView, String content) {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAppCacheEnabled(true);
        settings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                //progressBar.setVisibility(View.GONE);
                // 执行js
                //webView.loadUrl("javascript:funFromjs()");
            }

        });
        webView.setWebChromeClient(new WebChromeClient());

        webView.addJavascriptInterface(new JavaScriptObject(mContext), "Android");
        System.out.println(content);
        webView.loadDataWithBaseURL("http", content, "text/html", "utf-8", null);

    }

    /**
     * 给js调用的接口
     */
    public class JavaScriptObject {
        Context mContext;

        public JavaScriptObject(Context context) {
            this.mContext = context;
        }

        // 调用javascript
        /*@JavascriptInterface //sdk17版本以上加上注解
        public void fun1FromAndroid(String name) {
            Toast.makeText(mContext, name, Toast.LENGTH_LONG).show();
        }

        @JavascriptInterface //sdk17版本以上加上注解
        public void fun2(String name) {
            Toast.makeText(mContext, "调用fun2:" + name, Toast.LENGTH_SHORT).show();
        }*/

        @JavascriptInterface
        public void startPhotoSwipe(String url, String position, String imgsString) {
            String[] list = imgsString.split(" ");
            ArrayList arrayList = new ArrayList();
            for (int i = 0; i < list.length; i++) {
                arrayList.add(list[i]);
            }
            Intent intent = new Intent(mContext, PhotoViewActivity.class);
            intent.putStringArrayListExtra("imgs", arrayList);
            intent.putExtra("position", Integer.parseInt(position));  // 当前img的索引（从0开始）
            mContext.startActivity(intent);
        }

    }
}
