package com.xixi.chat.server.user;

import org.springframework.stereotype.Component;

import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;

/**
 * @author shengchengchao
 * @Description
 * @createTime 2021/9/15
 */
@Component
public class UserManager {

    private static final Map<String, String> userMap;

    private static final Map<String, SocketChannel> onlineUserChannel;

    static {
        userMap = new HashMap<>();
        onlineUserChannel = new HashMap<>();
        userMap.put("user1", "pwd1");
        userMap.put("user2", "pwd2");
        userMap.put("user3", "pwd3");
    }

    public boolean checkLogin(String userName, String pwd) {
        String s = userMap.getOrDefault(userName, "");
        return s.equals(pwd);
    }

    public void putChannel(String userName, SocketChannel channel) {
        onlineUserChannel.put(userName, channel);
    }


    public SocketChannel getChannel(String receiver) {
        return onlineUserChannel.getOrDefault(receiver, null);
    }
}
