package com.nowcoder.community.code;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

/**
 * @Author: 19599
 * @Date: 2025/9/2 2:22
 * @Description:
 */
public class MyThreadPool {

    private final List<Worker> workers;

    private final int capacity;

    private final BlockingQueue<Runnable> taskList;

    private boolean isRunning;

    public MyThreadPool(int capacity) {
        this.capacity = capacity;
        isRunning = true;
        taskList = new LinkedBlockingDeque<>(capacity);
        workers = new ArrayList<>(capacity);
    }

    public void execute(Runnable task) throws InterruptedException {
        if (isRunning) {
            throw new IllegalArgumentException("the thread pool has been closed");
        }
        if (workers.size() < capacity) {
            synchronized (workers) {
                if (workers.size() < capacity) {
                    workers.add(new Worker());
                }
            }
        }
        taskList.put(task);
    }

    public void stop() {
        isRunning = false;
        for (Worker worker : workers) {
            try {
                worker.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    class Worker extends Thread {
        @Override
        public void run() {
            while (isRunning || !taskList.isEmpty()) {
                Runnable task;
                try {
                    task = taskList.poll(10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    break;
                }
                if (task != null) {
                    try {
                        task.run();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}

