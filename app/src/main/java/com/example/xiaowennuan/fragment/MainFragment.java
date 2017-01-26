package com.example.xiaowennuan.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.xiaowennuan.R;
import com.example.xiaowennuan.base.ListBaseAdapter;
import com.example.xiaowennuan.db.ArticleModel;
import com.example.xiaowennuan.util.NetworkUtils;
import com.github.jdsjlzx.interfaces.OnItemClickListener;
import com.github.jdsjlzx.interfaces.OnLoadMoreListener;
import com.github.jdsjlzx.interfaces.OnRefreshListener;
import com.github.jdsjlzx.recyclerview.LRecyclerView;
import com.github.jdsjlzx.recyclerview.LRecyclerViewAdapter;
import com.github.jdsjlzx.recyclerview.ProgressStyle;
import com.github.jdsjlzx.util.RecyclerViewStateUtils;
import com.github.jdsjlzx.view.LoadingFooter;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.reflect.TypeToken;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import static android.content.Context.MODE_PRIVATE;


public class MainFragment extends Fragment {

    private static final int TYPE_IMAGE_NONE = 0;
    private static final int TYPE_IMAGE_RIGHT = 1;
    private static final int TYPE_IMAGE_WHOLE = 2;
    private static final int TYPE_IMAGE_THREE = 3;

    final String TAG = "MainFragment";

    /**服务器端一共多少条数据，此数值仅在刷新后改变，刷新的同时mCurrentCounter清零（加载更多不改变）*/
    private static int totalCounter = 0;

    /**每一页展示多少条数据*/
    private static final int REQUEST_COUNT = 10;

    /**已经获取到多少条数据了*/
    private static int mCurrentCounter = 0;
    /**最新时间戳*/
    private static int newestTs;

    // 请求的动作，分为refresh和loadmore
    private static String requestAction = "refresh";

    // widget
    private LRecyclerView mRecyclerView = null;

    private DataAdapter mDataAdapter = null;

    private ProgressBar progressBar;

    private LRecyclerViewAdapter mLRecyclerViewAdapter = null;

    private boolean isRefresh = false;
    private boolean isLoadMore = false;

    // check update
    private boolean firstLoad = true;
    private int updateCount;
    private int updateCountLimit = 10;  // 小于此数才直接更新

    private final int REFRESH = 1;
    private final int LOADMORE = 2;

    protected ArrayList<ArticleModel> mArticleList;  // 当前activity的文章列表
    protected ArrayList<ArticleModel> mNewList;  // loadmore每次请求的列表


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //Log.d("MainFragment", "MainFragment onCreateView");
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        FragmentActivity activity = getActivity();

        progressBar = (ProgressBar) view.findViewById(R.id.main_progressbar);
        //progressBar.setVisibility(View.VISIBLE);

        // Adapter
        mDataAdapter = new DataAdapter(activity);

        mRecyclerView = (LRecyclerView) view.findViewById(R.id.main_recycler_view);
        //Log.d(TAG, "valueof mRcyclerView:" + String.valueOf(mRecyclerView));

        mLRecyclerViewAdapter = new LRecyclerViewAdapter(mDataAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(activity));
        mRecyclerView.setAdapter(mLRecyclerViewAdapter);

        mRecyclerView.setRefreshProgressStyle(ProgressStyle.LineSpinFadeLoader);
        mRecyclerView.setArrowImageView(R.drawable.ic_pulltorefresh_arrow);

        //设置底部加载文字提示
        mRecyclerView.setHeaderViewColor(R.color.divider, R.color.black_overlay, R.color.white);
        mRecyclerView.setFooterViewHint("努力加载中...","已经到底了","网络不给力，点击重试");
        mRecyclerView.setFooterViewColor(R.color.divider, R.color.black_overlay, R.color.white);

        // Initialize Data
        initArticleData();

        mLRecyclerViewAdapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                //Log.d(TAG, "position: " + String.valueOf(position));
                Intent intent = new Intent("com.example.xiaowennuan.OPEN_ARTICLE");
                int mode = mArticleList.get(position).mode;
                final String ARTICLE = "com.example.xiaowennuan.ARTICLE";
                final String ARTICLE_MULTI_PHOTO = "com.example.xiaowennuan.ARTICLE_MULTI_PHOTO";
                switch (mode) {
                    case TYPE_IMAGE_RIGHT:
                        // 带折叠式toolbar
                        intent.addCategory(ARTICLE);
                        break;
                    case TYPE_IMAGE_WHOLE:
                        // 普通toolbar
                        intent.addCategory(ARTICLE_MULTI_PHOTO);
                        break;
                    case TYPE_IMAGE_THREE:
                        intent.addCategory(ARTICLE_MULTI_PHOTO);
                        break;
                    default:
                        intent.addCategory(ARTICLE_MULTI_PHOTO);
                }


                int aId = mArticleList.get(position).aId;
                intent.putExtra("aid", aId);
                intent.putExtra("category", "main");
                startActivity(intent);
            }
        });

        setOnRefresh();

        setLoadMore();

        return view;
    }

    private void setOnRefresh() {
        mRecyclerView.setOnRefreshListener(new OnRefreshListener() {
            @Override
            public void onRefresh() {
                mLRecyclerViewAdapter.removeHeaderView();
                mCurrentCounter = 0;
                isRefresh = true;
                requestAction = "refresh";
                requestData();
            }
        });
    }

    private void setLoadMore() {
        mRecyclerView.setOnLoadMoreListener(new OnLoadMoreListener() {
            @Override
            public void onLoadMore() {

                // 读取totalCounter
                SharedPreferences pref = getActivity().getSharedPreferences("article_data", MODE_PRIVATE);
                totalCounter = pref.getInt("totalCounter", 0);
                if (mCurrentCounter < totalCounter) {
                    // loading more
                    //RecyclerViewStateUtils.setFooterViewState(getActivity(), mRecyclerView, REQUEST_COUNT, LoadingFooter.State.Loading, null);
                    isLoadMore = true;
                    requestAction = "loadmore";
                    requestData();
                } else {
                    //the end
                    mRecyclerView.setNoMore(true);
                }
            }
        });
    }

    private void notifyDataSetChanged() {
        mLRecyclerViewAdapter.notifyDataSetChanged();

    }

    /**
     * 新数据插入并通知更新
     * @param list
     */
    private void addItems(ArrayList<ArticleModel> list) {
        mDataAdapter.addAll(list);
    }

    /**
     * 更新批量插入
     * @param list
     */
    private void insertItems(ArrayList<ArticleModel> list) {
        mDataAdapter.insertAll(list);
    }


    /**
     * 异步处理网络请求
     */
    private class PreviewHandler extends Handler {

        private WeakReference<FragmentActivity> ref;

        PreviewHandler(FragmentActivity activity) {
            ref = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            //Log.d(TAG, "handleMessage");
            final FragmentActivity activity = ref.get();
            if (activity == null || activity.isFinishing()) {
                return;
            }

            switch (msg.what) {
                case -1:  //网络可用
                    // 发起请求（并将新数据写入数据库），请求成功后还会调用initAriticleData()
                    queryFromServer();

                    notifyDataSetChanged();
                    break;
                case -2:
                    notifyDataSetChanged();
                    break;
                case -3: //网络不可用
                    if(isRefresh){
                        isRefresh = false;
                        mRecyclerView.refreshComplete();
                        Toast.makeText(activity, "网络不可用", Toast.LENGTH_LONG).show();
                    }
                    notifyDataSetChanged();
                    if(isLoadMore) {
                        RecyclerViewStateUtils.setFooterViewState(getActivity(), mRecyclerView,
                                REQUEST_COUNT, LoadingFooter.State.NetWorkError, mFooterClick);
                    }

                    break;
                default:
                    break;
            }
        }
    }


    /**
     * 检查文章更新
     */
    private void checkUpdateFromServer() {
        String queryAddress = getActivity().getString(R.string.domain_name) + "/articles/check_update/?newest_ts="
                + newestTs + "&cate=all";
        NetworkUtils.sendOkHttpRequest(queryAddress, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseText = response.body().string();
                // 处理数据，并写入数据库

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        final ArrayList<ArticleModel> list = handleCheckUpdateResponse(responseText);

                        //Log.d(TAG, "updateCount: " + String.valueOf(updateCount));
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (updateCount > 0 && updateCount < updateCountLimit) {
                                        insertItems(list);  // Data list 插入数据
                                        mArticleList.addAll(0, list);
                                        notifyDataSetChanged();  // 刷新 mLRecyclerViewAdapter
                                    } else if (updateCount >= updateCountLimit) {
                                        // 加入头部绑定点击事件
                                        View header = LayoutInflater.from(getActivity()).inflate(
                                                R.layout.notification_header, (ViewGroup) getView()
                                                        .findViewById(R.id.main_recycler_view), false);
                                        mLRecyclerViewAdapter.addHeaderView(header);
                                        notifyDataSetChanged();
                                        TextView textView = (TextView) header.findViewById(R.id.notification_header_text);
                                        textView.setText(String.valueOf(updateCount) + "篇新文章等你更新呦");
                                        textView.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View view) {
                                                mLRecyclerViewAdapter.removeHeaderView();
                                                mRecyclerView.forceToRefresh();
                                            }
                                        });
                                    } else if (updateCount == 0) {  // ==0
                                        // nothing will happen
                                    } else {  // ==-1
                                        Toast.makeText(getActivity(), "获取文章更新失败",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                        }

                    }
                }).start();

            }
        });
    }

    private ArrayList<ArticleModel> handleCheckUpdateResponse(String response) {
        ArrayList<ArticleModel> newList;
        if (!TextUtils.isEmpty(response)) {
            try {
                JsonObject jsonObject = new JsonParser().parse(response).getAsJsonObject();
                JsonArray jsonArray = jsonObject.getAsJsonArray("update_list");
                JsonPrimitive jsonPrimitive = jsonObject.getAsJsonPrimitive("update_count");

                updateCount = Integer.parseInt(jsonPrimitive.toString());
                Log.d(TAG, "update_count: " + String.valueOf(updateCount));

                if (updateCount > 0 && updateCount < updateCountLimit) {
                    // 直接更新list
                    Gson gson = new Gson();
                    newList = gson.fromJson(jsonArray, new TypeToken<ArrayList<ArticleModel>>() {}.getType());
                    Log.d(TAG, jsonArray.toString());
                    for (ArticleModel item : newList) {
                        ArticleModel ArticleModel = new ArticleModel();
                        ArticleModel.setaId(item.aId);
                        ArticleModel.setTitle(item.title);
                        ArticleModel.setContent(item.content);
                        ArticleModel.setDesc(item.desc);
                        ArticleModel.setMode(item.mode);
                        ArticleModel.setTimeStamp(item.timeStamp);
                        ArticleModel.setCategory(item.category);
                        ArticleModel.setImage1(item.image1);
                        ArticleModel.setImage2(item.image2);
                        ArticleModel.setImage3(item.image3);
                        ArticleModel.save();
                    }
                    // 记录文章总数
                    mCurrentCounter += updateCount;
                    totalCounter += updateCount;
                    SharedPreferences.Editor editor = getActivity().getSharedPreferences("article_data",
                            MODE_PRIVATE).edit();
                    editor.putInt("totalCounter", totalCounter);
                    editor.apply();
                    // 重新获取更新后的list，并绑定点击事件
                    //mArticleList = (ArrayList<ArticleModel>) mDataAdapter.getDataList();
                    //setOnItemClickListener();

                } else if (updateCount >= updateCountLimit) {
                    newList = null;
                    // 提示文章更新
                } else {  // ==0
                    newList = null;
                    // 保持不变
                }

                return newList;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        updateCount = -1;
        return null;  //服务器回复为空
    }


    /**
     * Query from server
     */
    private void queryFromServer() {
        // 从网络获取
        //Log.d(TAG, "queryFromServer-mCurrentCounter:" + mCurrentCounter);
        String queryAddress = getActivity().getString(R.string.domain_name) + "/articles/get_article_list/all/?action="
                + requestAction + "&request_count=" + REQUEST_COUNT + "&current_count=" +
                mCurrentCounter + "&newest_ts=" + newestTs;
        NetworkUtils.sendOkHttpRequest(queryAddress, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Message msg = new Message();
                if (isRefresh) {
                    isRefresh = false;  // 重置刷新状态
                    msg.what = REFRESH;
                } else if (isLoadMore) {
                    isLoadMore = false;
                    msg.what = LOADMORE;
                }
                loadFailedHanlder.sendMessage(msg);
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseText = response.body().string();
                // 处理数据，并写入数据库
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        final boolean result = handleArticleItemResponse(responseText);
                        if (result) {  // 请求成功
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    initArticleData();  // 再次请求本地数据库查询
                                }
                            });
                        } else {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(getActivity(), "获取文章列表失败",
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                }).start();

            }
        });
    }

    /**
     * 处理刷新失败
     */
    private Handler loadFailedHanlder = new Handler() {
        public void handleMessage(Message msg) {
            FragmentActivity activity = getActivity();
            Toast.makeText(activity, R.string.my_header_network_timeout, Toast.LENGTH_LONG).show();
            switch (msg.what) {
                case REFRESH:
                    mRecyclerView.refreshComplete();
                    break;
                case LOADMORE:
                    RecyclerViewStateUtils.setFooterViewState(getActivity(), mRecyclerView, REQUEST_COUNT, LoadingFooter.State.NetWorkError, mFooterClick);
                    break;
            }

        }

    };


    private View.OnClickListener mFooterClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            isLoadMore = true;
            RecyclerViewStateUtils.setFooterViewState(getActivity(), mRecyclerView, REQUEST_COUNT, LoadingFooter.State.Loading, null);
            requestData();
        }
    };

    /**
     * 异步初始化数据
     */
    private Handler initHandler = new Handler() {
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case 1: //refresh
                    mDataAdapter.setDataList(mArticleList);
                    isRefresh = false;
                    mRecyclerView.refreshComplete();
                    break;
                case 2: //loadmore
                    addItems(mNewList);  //调用了addAll方法，加载新的内容并更新总数
                    isLoadMore = false;
                    RecyclerViewStateUtils.setFooterViewState(mRecyclerView, LoadingFooter.State.Normal);
                    break;
                case 3:
                    progressBar.setVisibility(View.GONE);
                    mDataAdapter.setDataList(mArticleList);
                    break;
                default:

            }
        }
    };


    /**
     * 加载本地数据库数据，刷新和加载更多都会调用
     */
    private void initArticleData() {

        List<ArticleModel> items = DataSupport.order("timestamp desc").find(ArticleModel.class);
        mCurrentCounter = items.size();
        mArticleList = (ArrayList<ArticleModel>) items;

        Message message = new Message();
        if (isRefresh) {
            message.what = 1;
        } else if (isLoadMore) {
            message.what = 2;
        } else if (mCurrentCounter > 0) {
            message.what = 3;  //进入程序初始化并且有数据
        }

        initHandler.sendMessage(message);

        if (mCurrentCounter > 0) {
            newestTs = items.get(0).timeStamp;
            //Log.d(TAG, "newestTs：" + items.get(0).aId);
            if (firstLoad) {
                checkUpdateFromServer();
                firstLoad = false;
            }
        } else {
            requestData();
        }

    }



    /**
     *
     */
    private void requestData() {
        new Thread() {
            PreviewHandler mHandler = new PreviewHandler(getActivity());
            @Override
            public void run() {
                super.run();
                //根据网络情况发送message
                if(NetworkUtils.isNetAvailable(getContext())) {
                    mHandler.sendEmptyMessage(-1);
                } else {
                    mHandler.sendEmptyMessage(-3);
                }
            }
        }.start();
    }

    /**
     * 服务器查询得到数据，进行处理并存入（刷新）数据库
     */
    private boolean handleArticleItemResponse(String response) {
        if (!TextUtils.isEmpty(response)) {
            try {
                // 先转JsonObj
                Log.d(TAG, "response：" + response);
                JsonObject jsonObject = new JsonParser().parse(response).getAsJsonObject();
                JsonArray jsonArray = jsonObject.getAsJsonArray("article_list");
                JsonPrimitive jsonPrimitive = jsonObject.getAsJsonPrimitive("total_count");
                //Log.d("MyLog", "jsonArray::" + jsonArray.toString());
                Gson gson = new Gson();
                ArrayList<ArticleModel> newList = gson.fromJson(jsonArray, new TypeToken<ArrayList<ArticleModel>>() {}.getType());

                totalCounter = Integer.parseInt(jsonPrimitive.toString());

                if (this.isRefresh) {  // 刷新才更新总数并清空数据库
                    DataSupport.deleteAll(ArticleModel.class);

                } else if (this.isLoadMore){  // Load more
                    mNewList = newList;
                }
                for (ArticleModel item : newList) {
                    ArticleModel ArticleModel = new ArticleModel();
                    ArticleModel.setaId(item.aId);
                    ArticleModel.setTitle(item.title);
                    ArticleModel.setContent(item.content);
                    ArticleModel.setDesc(item.desc);
                    ArticleModel.setMode(item.mode);
                    ArticleModel.setTimeStamp(item.timeStamp);
                    ArticleModel.setCategory(item.category);
                    ArticleModel.setImage1(item.image1);
                    ArticleModel.setImage2(item.image2);
                    ArticleModel.setImage3(item.image3);
                    ArticleModel.save();
                }

                // 记录文章总数
                SharedPreferences.Editor editor = getActivity().getSharedPreferences("article_data",
                        MODE_PRIVATE).edit();
                editor.putInt("totalCounter", totalCounter);
                editor.apply();
                mCurrentCounter += newList.size();

                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * Adapter
     */
    private class DataAdapter extends ListBaseAdapter<ArticleModel> {

        public LayoutInflater mLayoutInflater;

        public DataAdapter(Context context) {
            mLayoutInflater = LayoutInflater.from(context);
            mContext = context;
        }

        @Override
        public List getDataList() {
            return mArticleList;
        }

        @Override
        public int getItemViewType(int position) {
            int viewType;
            switch (this.mDataList.get(position).mode) {
                case TYPE_IMAGE_NONE:
                    viewType = TYPE_IMAGE_NONE;
                    break;
                case TYPE_IMAGE_RIGHT:
                    viewType = TYPE_IMAGE_RIGHT;
                    break;
                case TYPE_IMAGE_WHOLE:
                    viewType = TYPE_IMAGE_WHOLE;
                    break;
                case TYPE_IMAGE_THREE:
                    viewType = TYPE_IMAGE_THREE;
                    break;
                default:
                    viewType = TYPE_IMAGE_NONE;
                    break;
            }
            return viewType;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (mContext ==null) {
                mContext = parent.getContext();
            }
            View view;
            RecyclerView.ViewHolder holder = null;
            switch (viewType) {
                case TYPE_IMAGE_NONE:
                    view = LayoutInflater.from(parent.getContext()).
                            inflate(R.layout.article_item_image_none, parent, false);
                    holder = new DataAdapter.ViewHolderImageNone(view);
                    break;
                case TYPE_IMAGE_RIGHT:
                    view = LayoutInflater.from(parent.getContext()).
                            inflate(R.layout.article_item_image_right, parent, false);
                    holder = new DataAdapter.ViewHolderImageRight(view);
                    break;
                case TYPE_IMAGE_WHOLE:
                    view = LayoutInflater.from(parent.getContext()).
                            inflate(R.layout.article_item_image_whole, parent, false);
                    holder = new DataAdapter.ViewHolderImageWhole(view);
                    break;
                case TYPE_IMAGE_THREE:
                    view = LayoutInflater.from(parent.getContext()).
                            inflate(R.layout.article_item_image_three, parent, false);
                    holder = new DataAdapter.ViewHolderImageThree(view);
                default:
            }
            return holder;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
            ArticleModel item = mDataList.get(position);
            int itemViewType = getItemViewType(position);
            //Log.d(TAG, "onBindViewHolder, type:" + itemViewType);
            switch (itemViewType) {
                case TYPE_IMAGE_NONE:
                    DataAdapter.ViewHolderImageNone holderImageNone = (DataAdapter.ViewHolderImageNone) holder;
                    holderImageNone.articleItemTitle.setText(item.title);
                    holderImageNone.articleItemTitle.setAlpha(0.87f);
                    holderImageNone.articleItemDesc.setText(item.desc);
                    holderImageNone.articleItemDesc.setAlpha(0.54f);
                    holderImageNone.articleItemCategory.setText(item.category);
                    break;
                case TYPE_IMAGE_RIGHT:
                    DataAdapter.ViewHolderImageRight holderImageRight = (DataAdapter.ViewHolderImageRight) holder;
                    holderImageRight.articleItemTitle.setText(item.title);
                    holderImageRight.articleItemTitle.setAlpha(0.87f);
                    holderImageRight.articleItemDesc.setText(item.desc);
                    holderImageRight.articleItemDesc.setAlpha(0.54f);
                    holderImageRight.articleCategory.setText(item.category);
                    Glide.with(mContext).load(item.image1).placeholder(R.drawable.placeholder_small)
                            .into(holderImageRight.articleItemImage);
                    break;
                case TYPE_IMAGE_WHOLE:
                    DataAdapter.ViewHolderImageWhole holderImageWhole = (DataAdapter.ViewHolderImageWhole) holder;
                    holderImageWhole.articleItemTitle.setText(item.title);
                    holderImageWhole.articleItemTitle.setAlpha(0.87f);
                    holderImageWhole.articleItemDesc.setText(item.desc);
                    holderImageWhole.articleItemDesc.setAlpha(0.54f);
                    holderImageWhole.articleCategory.setText(item.category);
                    Glide.with(mContext).load(item.image1).placeholder(R.drawable.placeholder_big)
                            .into(holderImageWhole.articleItemImage);
                    break;
                case TYPE_IMAGE_THREE:
                    DataAdapter.ViewHolderImageThree holderImageThree = (DataAdapter.ViewHolderImageThree) holder;
                    holderImageThree.articleItemTitle.setText(item.title);
                    holderImageThree.articleItemTitle.setAlpha(0.87f);
                    holderImageThree.articleItemDesc.setText(item.desc);
                    holderImageThree.articleItemDesc.setAlpha(0.54f);
                    holderImageThree.articleCategory.setText(item.category);
                    Glide.with(mContext).load(item.image1).placeholder(R.drawable.placeholder_small)
                            .into(holderImageThree.articleItemImage1);
                    Glide.with(mContext).load(item.image2).placeholder(R.drawable.placeholder_small)
                            .into(holderImageThree.articleItemImage2);
                    Glide.with(mContext).load(item.image3).placeholder(R.drawable.placeholder_small)
                            .into(holderImageThree.articleItemImage3);
                    break;
                default:
            }

        }

        private class ViewHolderImageNone extends RecyclerView.ViewHolder {
            TextView articleItemTitle;
            TextView articleItemDesc;
            TextView articleItemCategory;

            ViewHolderImageNone(View view) {
                super(view);
                articleItemTitle = (TextView) view.findViewById(R.id.article_item_image_none_title);
                articleItemDesc = (TextView) view.findViewById(R.id.article_item_image_none_desc);
                articleItemCategory = (TextView) view.findViewById(R.id.article_item_image_none_category);
            }
        }

        private class ViewHolderImageRight extends RecyclerView.ViewHolder {
            TextView articleItemTitle;
            TextView articleItemDesc;
            TextView articleCategory;
            ImageView articleItemImage;

            ViewHolderImageRight(View view) {
                super(view);
                articleItemTitle = (TextView) view.findViewById(R.id.article_item_image_right_title);
                articleItemDesc = (TextView) view.findViewById(R.id.article_item_image_right_desc);
                articleCategory = (TextView) view.findViewById(R.id.article_item_image_right_category);
                articleItemImage = (ImageView) view.findViewById(R.id.article_item_image_right_img);
            }
        }

        private class ViewHolderImageWhole extends RecyclerView.ViewHolder {
            TextView articleItemTitle;
            TextView articleItemDesc;
            TextView articleCategory;
            ImageView articleItemImage;

            public ViewHolderImageWhole(View view) {
                super(view);
                articleItemTitle = (TextView) view.findViewById(R.id.article_item_image_whole_title);
                articleItemDesc = (TextView) view.findViewById(R.id.article_item_image_whole_desc);
                articleCategory = (TextView) view.findViewById(R.id.article_item_image_whole_category);
                articleItemImage = (ImageView) view.findViewById(R.id.article_item_image_whole_img);
            }
        }

        private class ViewHolderImageThree extends RecyclerView.ViewHolder {
            TextView articleItemTitle;
            TextView articleItemDesc;
            TextView articleCategory;
            ImageView articleItemImage1;
            ImageView articleItemImage2;
            ImageView articleItemImage3;

            public ViewHolderImageThree(View view) {
                super(view);
                articleItemTitle = (TextView) view.findViewById(R.id.article_item_image_three_title);
                articleItemDesc = (TextView) view.findViewById(R.id.article_item_image_three_desc);
                articleCategory = (TextView) view.findViewById(R.id.article_item_image_three_category);
                articleItemImage1 = (ImageView) view.findViewById(R.id.article_item_image_three_img1);
                articleItemImage2 = (ImageView) view.findViewById(R.id.article_item_image_three_img2);
                articleItemImage3 = (ImageView) view.findViewById(R.id.article_item_image_three_img3);
            }
        }
    }

}
