package com.nowcoder.community.code;

import org.apache.lucene.util.CollectionUtil;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @Author: 19599
 * @Date: 2025/7/7 19:35
 * @Description: 来个手写线程池，要求：
 * 1. 可以复用线程
 * 2. 可以设置线程的最大数量
 * 3. 可以提交执行方法
 * 4. stop 的时候，要等所有的线程执行完毕，再关闭线程池
 */
public class MyThreadPoolExecutor {
    // 1. 任务队列：用于存储待执行的任务，需要是线程安全的
    private BlockingQueue<Runnable> taskQueue;

    // 2. 工作线程集合
    private final List<Worker> workers;

    // 3. 线程池最大线程数
    private final int maxPoolSize;

    // 4. 线程池是否正在运行的标志，volatile保证可见性
    private volatile boolean isRunning = true;

    public MyThreadPoolExecutor(int maxPoolSize) {
        if (maxPoolSize <= 0) {
            throw new IllegalArgumentException("max pool size must be positive");
        }
        this.maxPoolSize = maxPoolSize;
        this.taskQueue = new LinkedBlockingDeque<>();
        workers = new ArrayList<>();
    }

    /**
     * 提交执行任务的方法
     *
     * @param task 待执行的任务
     * @throws InterruptedException 如果在将任务放入队列时线程被中断
     */
    public void execute(Runnable task) throws InterruptedException {
        if (!isRunning) {
            throw new IllegalStateException("ThreadPoolExecutor is not running, can not execute the task~");
        }
        // 如果当前工作线程数小于最大线程数，则创建新线程
        // 这个检查和创建过程需要同步，防止并发执行时创建出超过maxPoolSize的线程
        if (workers.size() < maxPoolSize) {
            synchronized (workers) {
                if (workers.size() < maxPoolSize) {
                    Worker worker = new Worker();
                    workers.add(worker);
                    worker.start();
                    System.out.println("create a new worker: "
                            + worker.getName()
                            + ", current worker count: "
                            + workers.size());
                }
            }
        }
        // 将任务放入任务队列，如果队列满了，put方法会阻塞
        taskQueue.put(task);
    }

    public void stop() {
        System.out.println("--- Initiating ThreadPool shutdown ---");
        // 1. 停止接受新任务
        isRunning = false;
        // 2. 等待所有工作线程执行完毕，通过调用join()方法，主线程会阻塞直到每个工作线程都终止
        for (Worker worker : workers) {
            try {
                // 这是一个阻塞方法。
                // 当主线程对某个 worker 线程调用 worker.join() 时，主线程会暂停在这里，
                // 直到这个 worker 线程的 run() 方法执行完毕、线程完全终止后，join() 方法才会返回，主线程才能继续执行。
                worker.join();
            } catch (InterruptedException e) {
                /*
                 * 在主线程等待过程中，如果主线程被中断，则重新设置中断标志每个线程都有一个布尔类型的中断标志。
                 * 调用thread.interrupt()会将这个标志位设置为true
                 *
                 * 想象一下这个调用链：更高层的方法() -> pool.stop() -> worker.join()
                 * 中断发生：假设有一个更上层的代码决定要关闭整个应用，于是它调用了我们主线程的 interrupt() 方法。
                 * 主线程正在 worker.join() 处阻塞等待，它立刻收到了这个中断信号，于是抛出 InterruptedException。
                 * 同时，主线程的中断标志被清除了。
                 *
                 * 代码进入了 catch 块。
                 *
                 * 现在，如果我们不写 Thread.currentThread().interrupt();
                 * catch 块执行完毕后，stop() 方法可能会继续执行或者返回。
                 * 但是，那个最初的中断信号已经“消失”了。如果更高层的方法() 在 stop() 方法返回后，想检查一下中断状态
                 * 例如 if (Thread.currentThread().isInterrupted()) { ... }）来决定是否要执行后续的清理工作，
                 * 它会得到 false，因为它不知道中断曾经发生过。这个中断信号就被我们的 catch 块“吞掉”了。
                 * 而当我们加上 Thread.currentThread().interrupt(); 后：
                 * catch 块在处理完异常（比如打印日志）后，负责任地重新将中断标志位设置为 true。这相当于：
                 *
                 * 总结
                 * Thread.currentThread().interrupt() 的作用是传递和保留中断信号。
                 * 这是一种并发编程的黄金法则：
                 *      如果你捕获了 InterruptedException 并且你的方法不打算完全处理这个中断（即让线程就此终止），
                 *      那么你应该重新设置中断标志，除非你有非常充分的理由不这样做。
                 * */
                Thread.currentThread().interrupt();
                System.err.println("Main thread interrupted while waiting for worker to finish.");
            }
        }
        System.out.println("--- ThreadPool has been shut down ---");
    }

    /**
     * 工作线程内部类
     */
    private class Worker extends Thread {
        @Override
        public void run() {
            // 要求1：复用线程的关键所在，使用一个循环来不断地从任务队列中获取任务并执行
            // 循环退出的条件：线程池停止运行 并且 任务队列为空
            while (isRunning || !taskQueue.isEmpty()) {
                Runnable task = null;
                try {
                    // 从任务队列中获取任务
                    // 使用poll并设置超时，可以防止在线程池停止后，如果队列已空，线程会永远阻塞在take上
                    task = taskQueue.poll(1, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    // 如果在等待任务中被中断，就退出循环
                    System.err.println(Thread.currentThread().getName() + "was interrupted while waiting for task");
                    break;
                }

                if (task != null) {
                    try {
                        System.out.println(Thread.currentThread().getName() + "is executing a task");
                        task.run();
                    } catch (Exception e) {
                        // 捕获任务执行过程中的异常，防止因单个任务异常导致工作线程终止
                        System.err.println("Exception in task execution by "
                                + Thread.currentThread().getName()
                                + ":"
                                + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
            // 循环结束，线程自然终止
            System.out.println(Thread.currentThread().getName() + "is shutting down!");
        }
    }
}