package com.nowcoder.community.actuator;

import java.util.LinkedList;
import java.util.Queue;

/**
 * @Author: 19599
 * @Date: 2025/3/20 0:25
 * @Description:
 */
public abstract class MyInt {
    MyInt() {
        System.out.println("myint");
    }

    public static void main(String[] args) {
        Queue<String> que = new LinkedList<>();
        que.add("11");
        que.add("22");
        que.add(null);
        que.add("11");
        System.out.println(que);
    }
}
