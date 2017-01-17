package com.example.xiaowennuan.db;

import com.example.xiaowennuan.base.Entity;
import com.google.gson.annotations.SerializedName;

public class ArticleReadModel extends Entity {

    @SerializedName("aid")
    public int aId;

    @SerializedName("time_stamp")
    public int timeStamp;

    public String title;
    public String desc;
    public int mode;

    @SerializedName("cate")
    public String category;

    public String content;

    public String image1;
    public String image2;
    public String image3;

    public void setaId(int aId) {
        this.aId = aId;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public void setTimeStamp(int timeStamp) {
        this.timeStamp = timeStamp;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setImage1(String image1) {
        this.image1 = image1;
    }

    public void setImage2(String image2) {
        this.image2 = image2;
    }

    public void setImage3(String image3) {
        this.image3 = image3;
    }
}