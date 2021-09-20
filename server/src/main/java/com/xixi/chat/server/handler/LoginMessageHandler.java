package com.xixi.chat.server.handler;

import com.alibaba.fastjson.JSON;
import com.xixi.chat.common.dto.*;
import com.xixi.chat.common.enums.LoginEnum;
import com.xixi.chat.common.enums.ResponseEnum;
import com.xixi.chat.server.user.UserManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author shengchengchao
 * @Description
 * @createTime 2021/9/13
 */
@Component("login")
public class LoginMessageHandler extends MessageHandler {

    @Autowired
    private UserManager userManager;


    @Override
    public void handleMessage(Message message, SelectionKey client, AtomicInteger onlineUsers) throws Exception {
        MessageHeader header = message.getHeader();
        String userName = header.getSender();
        String pwd = new String(message.getBody());
        SocketChannel channel = (SocketChannel) client.channel();
        if (userManager.checkLogin(userName, pwd)) {
            userManager.putChannel(userName, channel);
            ResponseHeader responseHeader = new ResponseHeader();
            responseHeader.setSender(message.getHeader().getSender());
            responseHeader.setResponseCode(LoginEnum.LOGIN_SUCCESS.getCode());
            responseHeader.setTimestamp(message.getHeader().getTimestamp());
            responseHeader.setType(ResponseEnum.PROMPT.getCode());
            Response response = new Response();
            response.setBody(String.format(PromptMsgProperty.LOGIN_SUCCESS, onlineUsers.incrementAndGet()).getBytes(PromptMsgProperty.charset));
            response.setHeader(responseHeader);
            String s = JSON.toJSONString(response);
            byte[] bytes = s.getBytes(charset);
            channel.write(ByteBuffer.wrap(bytes));
            Thread.sleep(10);

            responseHeader.setSender("系统提示");
            responseHeader.setType(ResponseEnum.NORMAL.getCode());
            responseHeader.setResponseCode(LoginEnum.LOGIN_SUCCESS.getCode());
            response.setHeader(responseHeader);
            response.setBody(String.format(PromptMsgProperty.LOGIN_BROADCAST, message.getHeader().getSender()).getBytes(PromptMsgProperty.charset));
            s = JSON.toJSONString(response);
            super.broadcast(s.getBytes(), client.selector());
        } else {
            ResponseHeader responseHeader = new ResponseHeader();
            responseHeader.setSender(message.getHeader().getSender());
            responseHeader.setResponseCode(LoginEnum.LOGIN_FAILURE.getCode());
            responseHeader.setTimestamp(message.getHeader().getTimestamp());
            responseHeader.setType(ResponseEnum.PROMPT.getCode());
            Response response = new Response();
            response.setBody(String.format(PromptMsgProperty.LOGIN_FAILURE, onlineUsers.incrementAndGet()).getBytes(PromptMsgProperty.charset));
            response.setHeader(responseHeader);
            String s = JSON.toJSONString(response);
            byte[] bytes = s.getBytes(charset);
            channel.write(ByteBuffer.wrap(bytes));
        }
    }


}
