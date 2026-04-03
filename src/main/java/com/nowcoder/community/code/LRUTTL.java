package com.nowcoder.community.code;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @Author: 19599
 * @Date: 2025/6/28 19:55
 * @Description:
 */
public class LRUTTL {
    private static final Logger log = LoggerFactory.getLogger(LRUTTL.class);
    private final Map<Integer, Node> map;
    private final int capacity;
    private final Node head;
    private final Node tail;

    private final Lock lock = new ReentrantLock();

    public LRUTTL(int capacity) {
        this.capacity = capacity;
        this.map = new HashMap<>(capacity);
        head = new Node(-1, -1, -1);
        tail = new Node(-1, -1, -1);
        head.next = tail;
        tail.prev = head;
    }

    public int get(int key) {
        lock.lock();
        try {
            Node node = map.get(key);
            if (node == null) {
                return -1;
            }
            if (node.expireTime < System.currentTimeMillis()) {
                removeNode(node);
                map.remove(node.key);
                return -1;
            }
            moveToHead(node);
            return node.val;
        } finally {
            lock.unlock();
        }
    }

    public void put(int key, int val) {
        this.put(key, val, 0L);
    }

    public void put(int key, int val, long ttlMilles) {
        lock.lock();
        try {
            Node node = map.get(key);
            if (node != null) {
                node.val = val;
                node.expireTime = ttlMilles > 0 ? System.currentTimeMillis() + ttlMilles : Long.MAX_VALUE;
                moveToHead(node);
            } else {
                if (map.size() >= capacity) {
                    Node last = removeLast();
                    if (last != null) {
                        map.remove(last.key);
                    }
                }
                map.put(key, node);
                addToHead(node);
            }
        } finally {
            lock.unlock();
        }
    }

    private Node removeLast() {
        if (tail.prev == head) {
            return null;
        }
        Node last = tail.prev;
        removeNode(last);
        return last;
    }

    private void moveToHead(Node node) {
        removeNode(node);
        addToHead(node);
    }

    private void addToHead(Node node) {
        node.next = head.next;
        node.prev = head;
        head.next.prev = node;
        head.next = node;
    }

    private void removeNode(Node node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
    }


    static class Node {
        int key, val;
        Node prev, next;
        long expireTime;

        public Node(int val, int key, long expireTime) {
            this.expireTime = expireTime;
            this.val = val;
            this.key = key;
        }
    }
}
