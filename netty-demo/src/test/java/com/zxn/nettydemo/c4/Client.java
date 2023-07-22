package com.zxn.nettydemo.c4;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;

@Slf4j
public class Client {

    public static void main(String[] args) throws IOException {

        int i = 1;
        SocketChannel sc = SocketChannel.open();
        sc.connect(new InetSocketAddress("localhost", 8080));
//        do {
//
//            log.info("waiting...");
//            sc.write(Charset.defaultCharset().encode("" + i));
//
//            i++;
//
//        } while (i <= 10);

        System.out.println("beak");
    }
}
