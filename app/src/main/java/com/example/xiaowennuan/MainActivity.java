package com.example.xiaowennuan;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.xiaowennuan.db.ArticleRandomModel;
import com.example.xiaowennuan.fragment.MainFragment;
import com.example.xiaowennuan.fragment.NavHeartFragment;
import com.example.xiaowennuan.fragment.NavPhotoFragment;
import com.example.xiaowennuan.fragment.NavReadFragment;
import com.example.xiaowennuan.util.DoubleClickExitHelper;
import com.example.xiaowennuan.util.NetworkUtils;

import org.json.JSONObject;

import java.io.IOException;

import cn.sharesdk.framework.ShareSDK;
import okhttp3.Call;
import okhttp3.Response;

/**
 * 带HeaderView的分页加载LinearLayout RecyclerView
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MyLog";

    private DrawerLayout mDrawerLayout;

    private Fragment currentFragment;

    private final int RANDOM = 0;
    private int randomMode = 0;

    private int navItemId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        //Log.d(TAG, "Activity onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Toolbar
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // DrawerLayout
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBar actionBar = getSupportActionBar();  // 得到ActionBar（Toolbar）
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_menu_white_24dp);
        }
        mDrawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
            }

            @Override
            public void onDrawerOpened(View drawerView) {
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                // 当抽屉菜单完全关闭后，处理菜单点击事件
                switch (navItemId) {
                    case R.id.nav_home:
                        switchFragment(new MainFragment());
                        toolbar.setTitle(R.string.app_name);
                        break;
                    case R.id.nav_heart:
                        // initData and replace fragment
                        switchFragment(new NavHeartFragment());
                        toolbar.setTitle(R.string.nav_heart);
                        break;
                    case R.id.nav_read:
                        replaceFragment(new NavReadFragment());
                        toolbar.setTitle(R.string.nav_read);
                        break;
                    case R.id.nav_photo:
                        replaceFragment(new NavPhotoFragment());
                        toolbar.setTitle(R.string.nav_photo);
                        break;
                    case R.id.nav_rand:
                        Toast.makeText(MainActivity.this, "正在为你挑选文章", Toast.LENGTH_SHORT).show();
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                Message message = new Message();
                                message.what = RANDOM;
                                randHandler.sendMessage(message);
                            }

                        }).start();

                        //progressBar.setVisibility(View.VISIBLE);
                        break;
                    case R.id.nav_about:
                        Intent intentAbout = new Intent("com.example.xiaowennuan.OPEN_ABOUT");
                        startActivity(intentAbout);
                        break;
                    case R.id.nav_feedback:
                        Uri uri = Uri.parse("market://details?id=" + getPackageName());
                        Intent intentFeedback = new Intent(Intent.ACTION_VIEW, uri);
                        try {
                            startActivity(intentFeedback);
                        } catch (ActivityNotFoundException e) {
                            e.printStackTrace();
                        }
                        break;
                    default:
                }
            }

            @Override
            public void onDrawerStateChanged(int newState) {

            }
        });

        // NavigationView
        NavigationView navView = (NavigationView) findViewById(R.id.nav_view);
        navView.setItemIconTintList(null);
        navView.setCheckedItem(R.id.nav_home);  // 默认选中

        navView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {

            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {

                mDrawerLayout.closeDrawers();  // 关闭drawer
                // 菜单点击处理
                //Log.d(TAG, "getItemId:" + String.valueOf(item.getItemId()));
                navItemId = item.getItemId();

                return true;
            }


        });

        // SDKShare 初始化
        new Thread(new Runnable() {
            @Override
            public void run() {
                ShareSDK.initSDK(MainActivity.this);
            }
        });


        // Fragment initialization
        setDefaultFragment();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ShareSDK.stopSDK(this);
    }

    /**
     * random article handler
     */
    private Handler randHandler = new Handler() {
        public void handleMessage(Message msg) {
            String queryAddress = MainActivity.this.getString(R.string.domain_name)
                    + "/articles/get_rand_article/";
            switch (msg.what) {
                case RANDOM:
                    NetworkUtils.sendOkHttpRequest(queryAddress, new okhttp3.Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            e.printStackTrace();
                        }
                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            final String responseText = response.body().string();
                            // 处理数据，并写入数据库
                            final int mode = handleArticleItemResponse(responseText);
                            Intent intentRand = new Intent("com.example.xiaowennuan.OPEN_RANDOM_ARTICLE");
                            switch (mode) {
                                case 1:
                                    intentRand.addCategory("com.example.xiaowennuan.ARTICLE_RANDOM");
                                    break;
                                case 2:
                                case 3:
                                    intentRand.addCategory("com.example.xiaowennuan.ARTICLE_RANDOM_MULTI_PHOTO");
                                    break;
                                default:
                                    intentRand.addCategory("com.example.xiaowennuan.ARTICLE_RANDOM");
                            }
                            startActivity(intentRand);
                        }
                    });
                    break;
                default:
            }
            //progressBar.setVisibility(View.GONE);
        }
    };

    private int handleArticleItemResponse(String response) {
        if (!TextUtils.isEmpty(response)) {
            try {
                // 先转JsonObj
                Log.d(TAG, "response：" + response);
                JSONObject jsonObject = new JSONObject(response);
                ArticleRandomModel randomModel = new ArticleRandomModel();
                randomModel.setaId(jsonObject.getInt("aid"));
                randomModel.setTitle(jsonObject.getString("title"));
                randomModel.setContent(jsonObject.getString("content"));
                randomModel.setDesc(jsonObject.getString("desc"));
                randomModel.setMode(jsonObject.getInt("mode"));
                randomModel.setTimeStamp(jsonObject.getInt("time_stamp"));
                randomModel.setCategory(jsonObject.getString("cate"));
                randomModel.setImage1(jsonObject.getString("image1"));
                randomModel.setImage2(jsonObject.getString("image2"));
                randomModel.setImage3(jsonObject.getString("image3"));
                randomModel.save();
                randomMode = jsonObject.getInt("mode");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return randomMode;
    }

    private void setDefaultFragment() {
        //Log.d(TAG, "sefDefaultFragment");
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.main_fragment_layout, new MainFragment());
        transaction.commit();

    }

    private void replaceFragment(Fragment fragment) {
        //if (progressBar.getVisibility() == View.GONE) {
            //progressBar.setVisibility(View.VISIBLE);
        //}
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.main_fragment_layout, fragment);
        transaction.commit();
        //progressBar.setVisibility(View.GONE);
    }

    private void switchFragment(Fragment fragment) {
        //if (progressBar.getVisibility() == View.GONE) {
            //progressBar.setVisibility(View.VISIBLE);
        //}
        if (fragment != currentFragment) {
            if (currentFragment != null) {
                if (!fragment.isAdded()) {
                    getSupportFragmentManager().beginTransaction().hide(currentFragment)
                            .add(R.id.main_fragment_layout, fragment).commit();
                } else {
                    getSupportFragmentManager().beginTransaction().hide(currentFragment)
                            .show(fragment).commit();
                }
            } else {
                replaceFragment(fragment);
            }
            currentFragment = fragment;
        }
        //progressBar.setVisibility(View.GONE);
    }


    DoubleClickExitHelper doubleClick = new DoubleClickExitHelper(this);

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            return doubleClick.onKeyDown(keyCode,event);
        }
        return super.onKeyDown(keyCode, event);
    }

    /*@Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main_refresh, menu);
        return true;
    }*/

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            mDrawerLayout.openDrawer(GravityCompat.START);
        }
        return true;
    }
}