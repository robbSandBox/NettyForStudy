package com.zxn.nettydemo;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

public class TestFileChannelTransferTo {
    public static void main(String[] args) {
        try (
                FileChannel from = new FileInputStream("D:\\IdeaLocalE\\NettyForStudy\\netty-demo\\data.txt").getChannel();
                FileChannel to = new FileOutputStream("D:\\IdeaLocalE\\NettyForStudy\\netty-demo\\to.txt").getChannel()
        ) {
            //效率高,底层会利用操作系统的零拷贝进行优化,一次最多传2g
            long size = from.size();
            //left表示还有剩余多少字节
            for (long left = size; left > 0; ) {
                left -= from.transferTo(size-left, left, to);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
