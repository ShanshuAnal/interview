package com.nowcoder.community.code;

/**
 * @Author: 19599
 * @Date: 2025/9/9 18:23
 * @Description:
 */
public class DeadLock1 {
    public static void main(String[] args) {
        Task taskA = new Task(null);
        Task taskB = new Task(null);

        Thread threadA = new Thread(taskA, "线程A");
        Thread threadB = new Thread(taskB, "线程B");

        taskA.setTarget(threadB);
        taskB.setTarget(threadA);

        threadA.start();
        threadB.start();
    }

    /**
     * 自定义任务类
     */
    static class Task implements Runnable {

        private Thread target;

        public Task(Thread target) {
            this.target = target;
        }

        public void setTarget(Thread target) {
            this.target = target;
        }

        @Override
        public void run() {
            System.out.println(Thread.currentThread().getName() + "正在执行");
            if (target != null) {
                try {
                    target.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.println(Thread.currentThread().getName() + "执行完毕");
        }
    }
}
