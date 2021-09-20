package com.xixi.chat.common.enums;

/**
 * @author shengchengchao
 * @Description
 * @createTime 2021/9/13
 */
public enum ResponseEnum {

    NORMAL(1, "消息"),
    PROMPT(2, "提示"),
    FILE(3, "文件");

    private int code;
    private String desc;

    ResponseEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
}
