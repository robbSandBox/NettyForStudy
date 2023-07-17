package com.zxn.nettydemo;

import java.nio.ByteBuffer;

import static com.zxn.nettydemo.ByteBufferUtil.debugAll;

public class TestByteBufferRead {

    public static void main(String[] args) {
        ByteBuffer buffer = ByteBuffer.allocate(10);

        buffer.put(new byte[]{'a','b','c','d','e'});
        buffer.flip();

        buffer.get(new byte[4]);
        debugAll(buffer);
        buffer.rewind();
        System.out.println(((char) buffer.get()));
    }
}
