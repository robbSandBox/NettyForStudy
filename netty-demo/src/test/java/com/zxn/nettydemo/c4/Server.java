package com.zxn.nettydemo.c4;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.zxn.nettydemo.ByteBufferUtil.debugAll;
import static com.zxn.nettydemo.ByteBufferUtil.debugRead;


@Slf4j
public class Server {

    /**
     * 段注释是第二版
     */

    // 行注释是第二版
    public static void main(String[] args) throws IOException {
        /**
         * 1.创建selector,管理多个channel
         */
        Selector selector = Selector.open();

        accept(selector);
    }

    private static void accept(Selector selector) throws IOException {
        //使用nio来理解阻塞模式,单线程
        //1.创建服务器
        ServerSocketChannel ssc = ServerSocketChannel.open();

        ssc.configureBlocking(false); //设置服务器切换成非阻塞模式
        /**
         * 2.建立selector和channel之间的联系
         * selectionKey 就是将来事件发生后,通过这个key,获取事件信息
         *
         * 2.1 事件有四种类型
         * accept -->serverSocket独有,会在有连接请求时触发
         * connect --> 客户端独有,连接建立后触发
         * read --> 客户端发送服务器,可读事件
         * write --> 服务器到客户端,可写事件
         */
        SelectionKey sscKey = ssc.register(selector, 0, null);
        /**
         * 表名了ssc只关注连接连接事件,读写是sc关注的
         */
        sscKey.interestOps(SelectionKey.OP_ACCEPT);
        log.info("register key:{}", sscKey);
        //2,绑定端口
        ssc.bind(new InetSocketAddress(8080));

        while (true) {
            /**
             * 3.select方法是阻塞的,没有事件发生,线程阻塞,有事件发生,线程回复运行
             */
            selector.select();

            /**
             * 4.处理事件,内部包含了所有发生的事件
             */
            Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                /**
                 * 处理key的时候,注册上来的sc会加入set集合,当这个key对应的事件发生时,获取到这个事件,同时,还要删除这个key
                 */
                iterator.remove();
                log.info("key:{}", sscKey);
                /**
                 * 区分事件类型
                 */
                if (key.isAcceptable()) {
                    ServerSocketChannel channel = (ServerSocketChannel) key.channel();
                    SocketChannel sc = channel.accept();
                    sc.configureBlocking(false);
                    ByteBuffer buffer = ByteBuffer.allocate(16);
                    SelectionKey sckey = sc.register(selector, 0, buffer);
                    sckey.interestOps(SelectionKey.OP_READ);
                    log.info("{}", sc);
                    log.info("scKey{}", sckey);
                } else if (key.isReadable()) {
                    try {
                        SocketChannel sc = (SocketChannel) key.channel();
                        ByteBuffer buffer = (ByteBuffer) key.attachment();
                        int read = sc.read(buffer);
                        if (read == -1) {
                            key.cancel();
                        } else {
                            split(buffer);
                            if(buffer.position() == buffer.limit()){
                                ByteBuffer allocate = ByteBuffer.allocate(buffer.capacity() * 2);
                                buffer.flip();
                                allocate.put(buffer);
                                key.attach(allocate);
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        /**
                         * 客户端断开草会导致IO,所以当客户端断开后,应该也将key从selector的selectedKeys里面删除这个key
                         */
                        key.cancel();
                    }
                }
            }
        }
//        //3.创建连接集合
//        List<SocketChannel> channels = new ArrayList<>();
//
//        while (true){
//            //4.accept 建立与客户端的连接,SocketChannel用来与客户端通信
////            log.info("connecting...");
//            SocketChannel sc = ssc.accept();//阻塞方法,线程暂停
//            if(sc != null){
//                log.info("connected...{}",sc);
//                channels.add(sc);
//                sc.configureBlocking(false); //将channel设置成非阻塞,影响read方法
//            }
//
//            for (SocketChannel channel : channels) {
//
//                //5.接收客户端发送的数据
//                int read = channel.read(buffer);//阻塞方法
//                if (read > 0) {
//                    buffer.flip();
//                    debugRead(buffer);
//                    buffer.clear();
//                    log.info("after read...{}", channel);
//                }
//            }
//        }
    }

    private static List<ByteBuffer> split(ByteBuffer source) {
        source.flip();//切换为读
        List<ByteBuffer> all = new ArrayList<>();
        for (int i = 0; i < source.limit(); i++) {
            if (source.get(i) == '\n') {
                //新建buffer的长度就是遇到换行符的位置 - 当前读取位置
                int len = i + 1 - source.position();
                ByteBuffer target = ByteBuffer.allocate(len);

                for (int j = 0; j < len; j++) {
                    target.put(source.get());
                }
                all.add(target);
                debugAll(target);
            }
        }
        source.compact();//未读取的压缩到最前面
        return all;
    }
}
