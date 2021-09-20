package com.xixi.chat.common.dto;

import lombok.Data;


@Data
public class Message {
    private MessageHeader header;
    private byte[] body;


    public Message() {
    }
}
