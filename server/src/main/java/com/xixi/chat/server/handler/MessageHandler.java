package com.xixi.chat.server.handler;

import com.xixi.chat.common.dto.Message;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author shengchengchao
 * @Description
 * @createTime 2021/9/13
 */
@Slf4j
public abstract class MessageHandler {
    protected static Charset charset = Charset.forName("UTF8");


    abstract public void handleMessage(Message message, SelectionKey client, AtomicInteger onlineUsers) throws Exception;

    /**
     * 广播消息
     *
     * @param data
     * @param selector
     */
    protected void broadcast(byte[] data, Selector selector) {
        //得到所有的key
        try {
            for (SelectionKey selectedKey : selector.selectedKeys()) {
                SelectableChannel channel = selectedKey.channel();
                if (channel instanceof SocketChannel) {
                    SocketChannel dest = (SocketChannel) channel;
                    //在连接的情况下 写入数据
                    if (dest.isConnected()) {
                        dest.write(ByteBuffer.wrap(data));
                    }
                }
            }
        } catch (IOException e) {
            log.error(" MessageHeader.broadcast :发生异常", e);
        }
    }

}
