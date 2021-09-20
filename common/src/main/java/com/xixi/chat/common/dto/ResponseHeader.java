package com.xixi.chat.common.dto;

import lombok.Data;

/**
 * @author shengchengchao
 * @Description
 * @createTime 2021/9/13
 */
@Data
public class ResponseHeader {

    private String sender;
    private int type;
    private Integer responseCode;
    private Long timestamp;
}
