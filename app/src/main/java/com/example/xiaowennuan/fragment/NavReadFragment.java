package com.example.xiaowennuan.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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
import com.example.xiaowennuan.db.ArticleReadModel;
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


public class NavReadFragment extends Fragment {

    final String TAG = "ReadFragment";

    /**服务器端一共多少条数据，此数值仅在刷新后改变，刷新的同时mCurrentCounter清零（加载更多不改变）*/
    private static int totalCounter = 0;

    /**每一页展示多少条数据*/
    private static final int REQUEST_COUNT = 10;

    /**已经获取到多少条数据了*/
    private static int mCurrentCounter = 0;
    private static int newestTs;

    // 请求的动作，分为refresh和loadmore
    private static String requestAction = "refresh";

    private LRecyclerView mRecyclerView = null;

    private DataAdapter mDataAdapter = null;

    private LRecyclerViewAdapter mLRecyclerViewAdapter = null;

    private ProgressBar progressBar;

    private boolean isRefresh = false;

    private boolean isLoadMore = false;

    // 当前activity的文章列表
    protected ArrayList<ArticleReadModel> mArticleList;
    // loadmore每次请求的列表
    protected ArrayList<ArticleReadModel> mNewList;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //Log.d("ReadFragment", "ReadFragment onCreateView");
        View view = inflater.inflate(R.layout.fragment_nav_read, container, false);

        FragmentActivity activity = getActivity();

        progressBar = (ProgressBar) view.findViewById(R.id.nav_read_progressbar);

        // Adapter
        mDataAdapter = new DataAdapter(activity);

        mRecyclerView = (LRecyclerView) view.findViewById(R.id.nav_read_recycler_view);
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
                Intent intent = new Intent("com.example.xiaowennuan.OPEN_ARTICLE");
                intent.addCategory("com.example.xiaowennuan.ARTICLE");
                int aId = mArticleList.get(position).aId;
                intent.putExtra("aid", aId);
                intent.putExtra("category", "read");
                startActivity(intent);
            }

        });

        mRecyclerView.setOnRefreshListener(new OnRefreshListener() {
            @Override
            public void onRefresh() {
                //Log.d(TAG, "setOnRefreshListener");
                //RecyclerViewStateUtils.setFooterViewState(mRecyclerView, LoadingFooter.State.Normal);
                //mLRecyclerViewAdapter.notifyDataSetChanged();//fix bug:crapped or attached views may not be recycled. isScrap:false isAttached:true
                //mDataAdapter.clear();
                mCurrentCounter = 0;
                isRefresh = true;
                requestData();
            }
        });

        mRecyclerView.setOnLoadMoreListener(new OnLoadMoreListener() {
            @Override
            public void onLoadMore() {
                /*
                LoadingFooter.State state = RecyclerViewStateUtils.getFooterViewState(mRecyclerView);
                if(state == LoadingFooter.State.Loading) {
                    //Log.d(TAG, "the state is Loading, just wait..");
                    return;
                }
                */

                //Log.d(TAG, "mCurrentCounter:" + mCurrentCounter);
                //Log.d(TAG, "totalCounter:" + totalCounter);

                // 读取totalCounter
                SharedPreferences pref = getActivity().getSharedPreferences("article_data", MODE_PRIVATE);
                int totalCounter = pref.getInt("totalCounter", 0);

                if (mCurrentCounter < totalCounter) {
                    // loading more
                    //RecyclerViewStateUtils.setFooterViewState(getActivity(), mRecyclerView, REQUEST_COUNT, LoadingFooter.State.Loading, null);
                    isLoadMore = true;
                    requestAction = "loadmore";
                    requestData();
                } else {
                    //the end
                    mRecyclerView.setNoMore(true);
                    //RecyclerViewStateUtils.setFooterViewState(getActivity(), mRecyclerView, REQUEST_COUNT, LoadingFooter.State.TheEnd, null);
                }
            }
        });


        return view;
    }

    private void notifyDataSetChanged() {
        mLRecyclerViewAdapter.notifyDataSetChanged();
    }

    /**
     * 新数据插入并通知更新
     * @param list
     */
    private void addItems(ArrayList<ArticleReadModel> list) {
        mDataAdapter.addAll(list);
    }


    /**
     * 处理网络请求
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
                        RecyclerViewStateUtils.setFooterViewState(activity, mRecyclerView, REQUEST_COUNT, LoadingFooter.State.NetWorkError, mFooterClick);
                    }

                    break;
                default:
                    break;
            }
        }
    }


    /**
     * Query from server
     */
    private void queryFromServer() {
        // 从网络获取
        //Log.d(TAG, "queryFromServer-mCurrentCounter:" + mCurrentCounter);
        String queryAddress = getActivity().getString(R.string.domain_name) + "/articles/get_article_list/read/?action="
                + requestAction + "&request_count=" + REQUEST_COUNT + "&current_count=" +
                mCurrentCounter + "&newest_ts=" + newestTs;
        //showProgressDialog();
        //Log.d(TAG, queryAddress);
        NetworkUtils.sendOkHttpRequest(queryAddress, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseText = response.body().string();
                //Log.d("MyLog", responseText);
                // 处理数据，并写入数据库
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
        });
    }

    private View.OnClickListener mFooterClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
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

        List<ArticleReadModel> items = DataSupport.findAll(ArticleReadModel.class);
        mCurrentCounter = items.size();

        mArticleList = (ArrayList<ArticleReadModel>) items;

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
        } else {
            requestData();
        }

    }



    /**
     *
     */
    private void requestData() {
        //Log.d(TAG, "requestData");
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
                Log.d("MyLog", "jsonArray::" + jsonArray.toString());
                Gson gson = new Gson();
                ArrayList<ArticleReadModel> newList = gson.fromJson(jsonArray, new TypeToken<ArrayList<ArticleReadModel>>() {}.getType());

                totalCounter = Integer.parseInt(jsonPrimitive.toString());

                if (this.isRefresh) {  // 刷新才更新总数并清空数据库
                    DataSupport.deleteAll(ArticleReadModel.class);


                } else if (this.isLoadMore){  // Load more
                    mNewList = newList;
                }
                for (ArticleReadModel item : newList) {
                    ArticleReadModel ArticleReadModel = new ArticleReadModel();
                    ArticleReadModel.setaId(item.aId);
                    ArticleReadModel.setTitle(item.title);
                    ArticleReadModel.setContent(item.content);
                    ArticleReadModel.setDesc(item.desc);
                    ArticleReadModel.setMode(item.mode);
                    ArticleReadModel.setTimeStamp(item.timeStamp);
                    ArticleReadModel.setCategory(item.category);
                    ArticleReadModel.setImage1(item.image1);
                    ArticleReadModel.setImage2(item.image2);
                    ArticleReadModel.setImage3(item.image3);
                    ArticleReadModel.save();
                }

                // 记录文章总数
                SharedPreferences.Editor editor = getActivity().getSharedPreferences(
                        "article_read_data", MODE_PRIVATE).edit();
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
    private class DataAdapter extends ListBaseAdapter<ArticleReadModel> {

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
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (mContext ==null) {
                mContext = parent.getContext();
            }

            RecyclerView.ViewHolder holder = null;
            View view = LayoutInflater.from(parent.getContext()).
                    inflate(R.layout.article_item_image_right, parent, false);
            holder = new DataAdapter.ViewHolder(view);

            return holder;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
            ArticleReadModel item = mDataList.get(position);
            DataAdapter.ViewHolder viewHolder = (DataAdapter.ViewHolder) holder;
            viewHolder.articleItemTitle.setText(item.title);
            viewHolder.articleItemTitle.setAlpha(0.87f);
            viewHolder.articleItemDesc.setText(item.desc);
            viewHolder.articleItemDesc.setAlpha(0.54f);
            viewHolder.articleCategory.setText(item.category);
            Glide.with(mContext).load(item.image1).placeholder(R.drawable.placeholder_small)
                    .into(viewHolder.articleItemImage);

        }


        private class ViewHolder extends RecyclerView.ViewHolder {
            TextView articleItemTitle;
            TextView articleItemDesc;
            TextView articleCategory;
            ImageView articleItemImage;

            public ViewHolder(View view) {
                super(view);
                articleItemTitle = (TextView) view.findViewById(R.id.article_item_image_right_title);
                articleItemDesc = (TextView) view.findViewById(R.id.article_item_image_right_desc);
                articleCategory = (TextView) view.findViewById(R.id.article_item_image_right_category);
                articleItemImage = (ImageView) view.findViewById(R.id.article_item_image_right_img);
            }
        }


    }

}
