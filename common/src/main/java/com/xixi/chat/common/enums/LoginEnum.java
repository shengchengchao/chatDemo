package com.xixi.chat.common.enums;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by SinjinSong on 2017/5/23.
 */
public enum LoginEnum {
    LOGIN_SUCCESS(1, "登录成功"),
    LOGIN_FAILURE(2, "登录失败"),
    LOGOUT_SUCCESS(3, "下线成功");

    private int code;
    private String desc;

    LoginEnum(int code, String desc) {
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
