package com.xixi.chat.client;

import com.alibaba.fastjson.JSON;
import com.xixi.chat.common.dto.Message;
import com.xixi.chat.common.dto.MessageHeader;
import com.xixi.chat.common.dto.Response;
import com.xixi.chat.common.dto.ResponseHeader;
import com.xixi.chat.common.enums.LoginEnum;
import com.xixi.chat.common.util.DateTimeUtil;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

@Slf4j
public class ClientApplication extends Frame {
    public static final int DEFAULT_BUFFER_SIZE = 1024;
    private Selector selector;
    private SocketChannel clientChannel;
    private ByteBuffer buf;
    private TextField tfText;
    private TextArea taContent;
    private ReceiverHandler listener;
    private String username;
    private boolean isLogin = false;
    private boolean isConnected = false;
    private Charset charset = StandardCharsets.UTF_8;

    public ClientApplication(String name, int x, int y, int w, int h) {
        super(name);
        initFrame(x, y, w, h);
        initNetWork();
    }

    /**
     * 初始化窗体
     *
     * @param x
     * @param y
     * @param w
     * @param h
     */
    private void initFrame(int x, int y, int w, int h) {
        this.tfText = new TextField();
        this.taContent = new TextArea();
        this.setBounds(x, y, w, h);
        this.setLayout(new BorderLayout());
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                setVisible(false);
                System.exit(0);
            }
        });
        this.taContent.setEditable(false);
        this.add(tfText, BorderLayout.SOUTH);
        this.add(taContent, BorderLayout.NORTH);
        this.tfText.addActionListener((actionEvent) -> {
            String str = tfText.getText().trim();
            tfText.setText("");
            send(str);
        });
        this.pack();
        this.setVisible(true);
    }

    public void send(String content) {
        if (!isLogin) {
            JOptionPane.showMessageDialog(null, "尚未登录");
            return;
        }
        try {
            Message message;
            //普通模式
            if (content.startsWith("@")) {
                String[] slices = content.split(":");
                String receiver = slices[0].substring(1);
                message = new Message();
                message.setHeader(MessageHeader.builder()
                        .messageCode("normal")
                        .sender(username)
                        .receiver(receiver)
                        .timestamp(System.currentTimeMillis())
                        .build());
                message.setBody(slices[1].getBytes(charset));
            } else {
                //广播模式
                message = new Message();
                message.setHeader(MessageHeader.builder()
                        .messageCode("normal")
                        .sender(username)
                        .timestamp(System.currentTimeMillis())
                        .build());
                message.setBody(content.getBytes(charset));
            }
            System.out.println(message);
            clientChannel.write(ByteBuffer.wrap(JSON.toJSONBytes(message)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initNetWork() {
        try {
            selector = Selector.open();
            clientChannel = SelectorProvider.provider().openSocketChannel();
            clientChannel.connect(new InetSocketAddress("127.0.0.1", 9000));
            clientChannel.configureBlocking(false);
            clientChannel.register(selector, SelectionKey.OP_READ);
            buf = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE);
            login();
            isConnected = true;
            isLogin = true;
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void login() {
        String username = JOptionPane.showInputDialog("请输入用户名");
        String password = JOptionPane.showInputDialog("请输入密码");
        Message message = new Message();
        MessageHeader login = MessageHeader.builder()
                .messageCode("login")
                .sender(username)
                .timestamp(System.currentTimeMillis())
                .build();
        message.setHeader(login);
        message.setBody(password.getBytes(charset));
        String s = JSON.toJSONString(message);
        try {
            clientChannel.write(ByteBuffer.wrap(s.getBytes(charset)));
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.username = username;
    }

    public static void main(String[] args) {
        System.out.println("Initialing...");
        ClientApplication client = new ClientApplication("Client", 200, 200, 300, 200);
        client.launch();
    }

    private void launch() {
        this.listener = new ReceiverHandler();
        new Thread(listener).start();
    }


    private class ReceiverHandler implements Runnable {

        private Boolean connected = true;

        @Override
        public void run() {

            try {
                while (connected) {
                    int size = 0;
                    selector.select();
                    for (Iterator<SelectionKey> iterator = selector.selectedKeys().iterator(); iterator.hasNext(); ) {
                        SelectionKey next = iterator.next();
                        iterator.remove();
                        if (next.isReadable()) {
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            while ((size = clientChannel.read(buf)) > 0) {
                                buf.flip();
                                baos.write(buf.array(), 0, size);
                                buf.clear();
                            }
                            byte[] bytes = baos.toByteArray();
                            baos.close();
                            Response response = JSON.parseObject(bytes, Response.class);
                            handler(response);

                        }
                    }
                }
            } catch (Exception e) {
                log.error(" ReceiverHandler.run :发生异常", e);
            }

        }

        private void handler(Response response) {
            System.out.println(response);

            ResponseHeader header = response.getHeader();

            switch (header.getType()) {
                case 2:


                    Integer code = header.getResponseCode();
                    if (code == LoginEnum.LOGIN_SUCCESS.getCode()) {
                        isLogin = true;
                        System.out.println("登录成功");
                    } else if (code == LoginEnum.LOGIN_FAILURE.getCode()) {
                        System.out.println("下线成功");
                        break;
                    }
                    String info = new String(response.getBody(), charset);
                    JOptionPane.showMessageDialog(ClientApplication.this, info);
                case 1:
                    String content = formatMessage(taContent.getText(), response);
                    taContent.setText(content);
                    taContent.setCaretPosition(content.length());
                    break;
            }
        }

        private String formatMessage(String originalText, Response response) {
            ResponseHeader header = response.getHeader();
            StringBuilder sb = new StringBuilder();
            sb.append(originalText)
                    .append(header.getSender())
                    .append(": ")
                    .append(new String(response.getBody(), charset))
                    .append("    ")
                    .append(DateTimeUtil.formatLocalDateTime(header.getTimestamp()))
                    .append("\n");
            return sb.toString();
        }
    }
}
