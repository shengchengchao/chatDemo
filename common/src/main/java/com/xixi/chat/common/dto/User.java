package com.xixi.chat.common.dto;

import lombok.Data;

import java.nio.channels.SocketChannel;


@Data
public class User {
    private String username;
    private String password;
    private SocketChannel channel;

}
