package com.nowcoder.community.code;

/**
 * @Author: 19599
 * @Date: 2025/6/23 21:24
 * @Description:
 */
public class DeadLock {
    private static final Object lock1 = new Object();
    private static final Object lock2 = new Object();

    public static void main(String[] args) {
        Thread thread1 = new Thread(() -> {
            synchronized (lock1) {
                System.out.println("线程1 持有 lock1");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                synchronized (lock2) {
                    System.out.println("线程1 持有 lock2");
                }
            }
        }, "线程1");

        Thread thread2 = new Thread(() -> {
            synchronized (lock2) {
                System.out.println("线程2 持有 lock2");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                synchronized (lock1) {
                    System.out.println("线程2 持有 lock1");
                }
            }
        }, "线程2");

        thread1.start();
        thread2.start();
    }
}