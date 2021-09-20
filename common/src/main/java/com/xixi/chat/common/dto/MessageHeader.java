package com.xixi.chat.common.dto;

import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class MessageHeader {
    private String sender;
    private String receiver;
    private Long timestamp;
    private String messageCode;

    public MessageHeader() {
    }

    public MessageHeader(String sender, String receiver, Long timestamp, String messageCode) {
        this.sender = sender;
        this.receiver = receiver;
        this.timestamp = timestamp;
        this.messageCode = messageCode;
    }
}
