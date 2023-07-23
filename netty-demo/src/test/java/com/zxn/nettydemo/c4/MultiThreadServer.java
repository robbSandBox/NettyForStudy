package com.zxn.nettydemo.c4;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import static com.zxn.nettydemo.ByteBufferUtil.debugAll;

@Slf4j
public class MultiThreadServer {

    public static void main(String[] args) throws IOException {
        Thread.currentThread().setName("Boss");

        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.configureBlocking(false);

        Selector boss = Selector.open();
        ssc.register(boss, SelectionKey.OP_ACCEPT, null);

        ssc.bind(new InetSocketAddress(8080));
        //1.创建固定数量的worker

        Worker[] workers = new Worker[Runtime.getRuntime().availableProcessors()];

        for (int i = 0; i < workers.length; i++) {
            workers[i] = new Worker("worker-" + i);
        }
        Worker worker = new Worker("worker-0");

        AtomicInteger index = new AtomicInteger();
        while (true) {
            boss.select();

            Iterator<SelectionKey> iter = boss.selectedKeys().iterator();
            while (iter.hasNext()) {
                SelectionKey key = iter.next();
                iter.remove();
                if (key.isAcceptable()) {
                    SocketChannel sc = ssc.accept();
                    sc.configureBlocking(false);
                    log.info("建立连接--->{}", sc.getRemoteAddress());

                    log.info("开始往worker注册--->{}", sc.getRemoteAddress());
                    //2.boss发现有新链接之后,让其关联worker的selector
                    workers[index.getAndIncrement() % workers.length].register(sc);
                    log.info("注册到worker成功--->{}", sc.getRemoteAddress());
                }
            }
        }
    }

    /**
     * https://www.bilibili.com/video/BV1py4y1E7oA/?p=44&spm_id_from=pageDriver&vd_source=a936d6cd836d87ce45c85e1f61d92f3b
     */

    static class Worker implements Runnable {
        private Thread thread;
        private Selector selector;
        private String name;
        private volatile boolean start = false;
        private ConcurrentLinkedQueue<Runnable> queue = new ConcurrentLinkedQueue<>();
        public Worker(String name) {
            this.name = name;
        }

        //初始化线程和selector
        public void register(SocketChannel sc) throws IOException {
            if (!start) {
                selector = Selector.open();
                thread = new Thread(this, name);
                thread.start();
                start = true;
            }

            //向队列添加任务,但任务还未执行
            queue.add(() ->{
                try {
                    sc.register(selector, SelectionKey.OP_READ, null);
                } catch (ClosedChannelException e) {
                    e.printStackTrace();
                }
            });

            selector.wakeup();
        }

        @Override
        public void run() {
            while (true) {
                try {
                    this.selector.select();
                    Runnable task = queue.poll();

                    if(task != null){
                        task.run();
                    }

                    Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
                    if (iter.hasNext()) {
                        SelectionKey key = iter.next();
                        iter.remove();

                        if (key.isReadable()) {
                            ByteBuffer buffer = ByteBuffer.allocate(16);

                            SocketChannel sc = (SocketChannel) key.channel();
                            sc.configureBlocking(false);
                            log.info("{}读取到数据--->{}", this.name, sc.getRemoteAddress());
                            sc.read(buffer);

                            buffer.flip();
                            debugAll(buffer);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
