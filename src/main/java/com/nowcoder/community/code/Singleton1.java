package com.nowcoder.community.code;

/**
 * @Author: 19599
 * @Date: 2025/6/25 22:21
 * @Description:
 */
public class Singleton1 {
    private volatile static Singleton1 instance;

    private Singleton1() {
    }

    public static Singleton1 getInstance() {
        if (instance == null) {
            synchronized (Singleton1.class) {
                if (instance == null) {
                    instance = new Singleton1();
                }
            }
        }
        return instance;
    }
}
