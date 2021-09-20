package com.xixi.chat.server.thread;

import com.alibaba.fastjson.JSON;
import com.xixi.chat.common.dto.Message;
import com.xixi.chat.common.dto.MessageHeader;
import com.xixi.chat.common.util.ApplicationContextUtil;
import com.xixi.chat.server.handler.MessageHandler;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author shengchengchao
 * @Description
 * @createTime 2021/9/12
 */
@Slf4j
public class ListenerThread extends Thread {

    private Selector selector;

    private ThreadPoolExecutor threadPool;

    private ServerSocketChannel serverSocketChannel;
    private AtomicInteger onlineUsers;

    public static final int DEFAULT_BUFFER_SIZE = 1024;

    public ListenerThread(Selector selector, ThreadPoolExecutor threadPool, AtomicInteger users, ServerSocketChannel channel) {
        this.selector = selector;
        this.threadPool = threadPool;
        onlineUsers = users;
        serverSocketChannel = channel;
    }


    @Override
    public void run() {

        try {
            System.out.println(1);
            //阻塞 有数据才返回
            selector.select();
            for (Iterator<SelectionKey> it = selector.selectedKeys().iterator(); it.hasNext(); ) {
                SelectionKey next = it.next();
                it.remove();
                if (next.isAcceptable()) {
                    //处理接口请求
                    handleAcceptRequest();
                } else if (next.isReadable()) {
                    //处理可读的请求
                    threadPool.execute(() -> executeMessage(next));
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void handleAcceptRequest() {
        try {
            SocketChannel client = serverSocketChannel.accept();
            // 接收的客户端也要切换为非阻塞模式
            client.configureBlocking(false);
            // 监控客户端的读操作是否就绪
            client.register(selector, SelectionKey.OP_READ);
            log.info("服务器连接客户端:{}", client);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void executeMessage(SelectionKey key) {
        // 得到对应的频道 以及初始化一块缓存区
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            SocketChannel channel = (SocketChannel) key.channel();
            ByteBuffer allocate = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE);
            int size;
            while ((size = channel.read(allocate)) > 0) {
                // 将指针放到开头
                allocate.flip();
                byteArrayOutputStream.write(allocate.array(), 0, size);
                allocate.clear();
                if (size == -1) {
                    return;
                }
                //表示可以继续监听数据
                key.interestOps(key.interestOps() | SelectionKey.OP_READ);
                // 唤醒阻塞的selector
                key.selector().wakeup();
                byte[] bytes = byteArrayOutputStream.toByteArray();
                byteArrayOutputStream.close();
                Message message = JSON.parseObject(new String(bytes), Message.class);
                MessageHeader header = message.getHeader();
                MessageHandler handler = ApplicationContextUtil.getBean(header.getMessageCode());
                handler.handleMessage(message, key, onlineUsers);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
