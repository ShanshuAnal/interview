package com.nowcoder.community.code;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author: 19599
 * @Date: 2025/7/2 22:33
 * @Description:
 */
public class CASCounter {
    private final AtomicInteger count = new AtomicInteger(0);

    public void increment() {
        // 这就是CAS自旋
        while (true) {
            // 1. 读取当前最新值
            int expected = count.get();
            // 2. 计算新值
            int newValue = expected + 1;
            // 执行CAS操作
            // 尝试用原子方式，将count的值从expectedValue更新为newValue
            if (count.compareAndSet(expected, newValue)) {
                // 如果成功，说明在操作期间没有其他线程修改count
                // 直接跳出循环
                break;
            }
            // 如果失败，说明在读取get和尝试设置compareAndSet之间
            // 有其他线程已经把count的值修改了
            // CAS操作失败，循环会继续，重复读取最近的值，再次尝试
        }
    }

    private int getCount() {
        increment();
        return count.get();
    }

    public static void main(String[] args) {
        CASCounter casCounter = new CASCounter();

        new Thread(() -> {
            int count = 1;
            while (count++ < 10) {
                System.out.println(Thread.currentThread().getName() + casCounter.getCount());
            }
        }, "thread1-").start();

        new Thread(() -> {
            int count = 1;
            while (count++ < 10) {
                System.out.println(Thread.currentThread().getName() + casCounter.getCount());
            }
        }, "thread2-").start();
    }
}
