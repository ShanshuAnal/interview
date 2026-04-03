package com.nowcoder.community.code;

/**
 * @Author: 19599
 * @Date: 2025/6/25 22:22
 * @Description:
 */
public class Singleton2 {
    private Singleton2() {

    }

    private static class Singleton2Holder {
        private static final Singleton2 INSTANCE = new Singleton2();
    }

    public static Singleton2 getInstance() {
        return Singleton2Holder.INSTANCE;
    }


}
