package com.xixi.chat.server.handler;

import com.alibaba.fastjson.JSON;
import com.xixi.chat.common.dto.*;
import com.xixi.chat.common.enums.ResponseEnum;
import com.xixi.chat.server.user.UserManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author shengchengchao
 * @Description
 * @createTime 2021/9/15
 */
@Component("normal")
@Slf4j
public class NormalMessageHandler extends MessageHandler {

    @Autowired
    private UserManager userManager;

    @Override
    public void handleMessage(Message message, SelectionKey client, AtomicInteger onlineUsers) throws Exception {
        SocketChannel channel = (SocketChannel) client.channel();
        MessageHeader header = message.getHeader();
        SocketChannel receiverChannel = userManager.getChannel(header.getReceiver());
        if (receiverChannel == null) {
            ResponseHeader responseHeader = new ResponseHeader();
            responseHeader.setSender(message.getHeader().getSender());
            responseHeader.setTimestamp(message.getHeader().getTimestamp());
            responseHeader.setType(ResponseEnum.PROMPT.getCode());
            Response response = new Response();
            response.setBody(String.format(PromptMsgProperty.LOGOUT_BROADCAST).getBytes(PromptMsgProperty.charset));
            response.setHeader(responseHeader);
            String s = JSON.toJSONString(response);
            byte[] bytes = s.getBytes(charset);
            channel.write(ByteBuffer.wrap(bytes));
        } else {
            ResponseHeader responseHeader = new ResponseHeader();
            responseHeader.setSender(message.getHeader().getSender());
            responseHeader.setTimestamp(message.getHeader().getTimestamp());
            responseHeader.setType(ResponseEnum.PROMPT.getCode());
            Response response = new Response();
            response.setHeader(responseHeader);
            String s = JSON.toJSONString(response);
            byte[] bytes = s.getBytes(charset);

            log.info(" NormalMessageHandler.handleMessage 已发给{} ", receiverChannel);
            receiverChannel.write(ByteBuffer.wrap(bytes));
            channel.write(ByteBuffer.wrap(bytes));

        }

    }
}
