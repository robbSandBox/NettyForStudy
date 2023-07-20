package com.zxn.nettydemo;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.zxn.nettydemo.ByteBufferUtil.debugAll;

public class TestByteBufferExam {

    public static void main(String[] args) {
        /**
         * 网络上有多条数据发送给服务器,数据之间使用\n进行分割
         * 但是由于某种原因导致这些数据在接收时,被进行了重新组合,例如原始数据有3条,为:
         * Hello,world\n
         * I`m zhangsan\n
         * How are you?\n
         * 变成了下面两个bytebuffer(粘包,半包)
         * Hello,world\nI`m zhangsan\nHo //粘包,效率高 快递员送货
         * w are you?\n //半包,有缓冲区大小决定,粘包占据了整个缓冲区,多于的进行第二次传输,产生半包
         * 现在要求你编写程序,将错乱的数据恢复成原始的按\n分割的数据
         */

        ByteBuffer source = ByteBuffer.allocate(32);
        source.put("Hello,world\nI`m zhangsan\nHo".getBytes(StandardCharsets.UTF_8));
        List<ByteBuffer> split = split(source);
        source.put("w are you?\n".getBytes(StandardCharsets.UTF_8));
        List<ByteBuffer> split1 = split(source);
        split.addAll(split1);

        split.forEach(s->{
            byte[] array = s.array();
//            System.out.println(Arrays.toString(array));
        });
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
