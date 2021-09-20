package com.xixi.chat.server;


import com.alibaba.fastjson.JSON;
import com.xixi.chat.common.dto.Message;
import com.xixi.chat.common.util.ApplicationContextUtil;
import com.xixi.chat.server.handler.MessageHandler;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class ServerApplication {
    public static final int PORT = 9000;

    private ServerSocketChannel serverSocketChannel;
    private Selector selector;
    private ThreadPoolExecutor threadPool;
    private AtomicInteger onlineUsers;


    public ServerApplication() {
        log.info("服务器启动");
        initServer();
        launch();
    }

    private void initServer() {

        try {
            // 1。得到一个channel
            serverSocketChannel = ServerSocketChannel.open();
            //2.切换成非阻塞模式
            serverSocketChannel.configureBlocking(false);
            //3.关联端口
            serverSocketChannel.bind(new InetSocketAddress(PORT));

            selector = Selector.open();
            //4. 将channel 注册到selector  选择接收模式  到此基本完成绑定
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            //5.生成 线程池
            threadPool = new ThreadPoolExecutor(5, 10, 1000, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(10), new ThreadPoolExecutor.CallerRunsPolicy());
            // 计算在线人数
            onlineUsers = new AtomicInteger(0);


        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    public void launch() {
        new Thread(new ListenerThread()).start();
    }

    private class ListenerThread extends Thread {


        @Override
        public void run() {
            try {
                //如果有一个及以上的客户端的数据准备就绪
                while (!Thread.currentThread().isInterrupted()) {
                    System.out.println(1);
                    //当注册的事件到达时，方法返回；否则,该方法会一直阻塞
                    selector.select();
                    //获取当前选择器中所有注册的监听事件
                    for (Iterator<SelectionKey> it = selector.selectedKeys().iterator(); it.hasNext(); ) {
                        SelectionKey key = it.next();
                        //删除已选的key,以防重复处理
                        it.remove();
                        //如果"接收"事件已就绪 一开始启动的接收事件就是 server的selector 接收的事件处理起
                        if (key.isAcceptable()) {
                            //交由接收事件的处理器处理
                            handleAcceptRequest();
                        } else if (key.isReadable()) {
                            //如果"读取"事件已就绪
                            //取消可读触发标记，本次处理完后才打开读取事件标记  这里的数据是指取消可读的处理
                            key.interestOps(key.interestOps() & (~SelectionKey.OP_READ));
                            //交由读取事件的处理器处理
                            threadPool.execute(new ReadEventHandler(key));
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
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

    private class ReadEventHandler implements Runnable {

        private ByteBuffer buf;
        private SocketChannel client;
        private ByteArrayOutputStream baos;
        private SelectionKey key;

        public ReadEventHandler(SelectionKey key) {
            this.key = key;
            this.client = (SocketChannel) key.channel();
            this.buf = ByteBuffer.allocate(1024);
            this.baos = new ByteArrayOutputStream();
        }

        @Override
        public void run() {
            try {
                int size;

                while ((size = client.read(buf)) > 0) {
                    buf.flip();
                    baos.write(buf.array(), 0, size);
                    buf.clear();
                }
                if (size == -1) {
                    return;
                }
                log.info("读取完毕，继续监听");
                //继续监听读取事件  继续进行读取
                key.interestOps(key.interestOps() | SelectionKey.OP_READ);
                key.selector().wakeup();
                byte[] bytes = baos.toByteArray();
                baos.close();
                Message message = JSON.parseObject(new String(bytes), Message.class);
                MessageHandler handler = ApplicationContextUtil.getBean(message.getHeader().getMessageCode());
                try {
                    handler.handleMessage(message, key, onlineUsers);
                } catch (Exception e) {
                    log.error("服务器线程被中断");
                    e.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }


    public static void main(String[] args) {

        System.out.println("Initialing...");

        ServerApplication chatServer = new ServerApplication();
        Scanner scanner = new Scanner(System.in, "UTF-8");
        while (scanner.hasNext()) {
            String next = scanner.next();
            if (next.equalsIgnoreCase("QUIT")) {
                System.out.println("服务器准备关闭");
                System.out.println("服务器已关闭");
            }
        }
    }

}
