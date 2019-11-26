package com.srchardcodeutil.bean;

/**
 * 资源替换实体类
 * Created by bauer on 2019/11/25.
 */
public class Entity {
    private String id;
    private String value;

    public Entity() {
    }

    public Entity(String id, String value) {
        this.id = id;
        this.value = value;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}