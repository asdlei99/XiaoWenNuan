package com.example.xiaowennuan.base;


import org.litepal.crud.DataSupport;

import java.io.Serializable;

/**
 * 实体类
 * 
 */
public abstract class Entity extends DataSupport implements Serializable {
    public int id;
    public int type; // content type

}
