package com.xixi.chat.common.dto;

import lombok.Data;

/**
 * @author shengchengchao
 * @Description
 * @createTime 2021/9/13
 */
@Data
public class Response {

    private ResponseHeader header;
    private byte[] body;
}
