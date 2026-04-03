package com.nowcoder.community.code;

import java.util.Arrays;
import java.util.Comparator;

/**
 * @Author: 19599
 * @Date: 2025/7/3 21:12
 * @Description:
 */
@SuppressWarnings("unchecked")
public class MaxHeap<T> {
    private Object[] heap;
    private int capacity;
    private int size;
    private final Comparator<T> comparator;

    public MaxHeap(int capacity, Comparator<T> comparator) {
        this.capacity = capacity;
        this.heap = new Object[Math.max(1, capacity)];
        this.size = 0;
        this.comparator = comparator;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public T peek() {
        if (isEmpty()) {
            throw new RuntimeException("heap is empty");
        }
        return (T) heap[0];
    }

    public void offer(T val) {
        if (size == capacity) {
            resize();
        }
        heap[size] = val;
        siftUp(size);
        size++;
    }

    private void siftUp(int i) {
        while (i > 0) {
            int parent = (i - 1) / 2;
            if (comparator.compare((T) heap[parent], (T) heap[i]) >= 0) {
                break;
            }
            swap(parent, i);
            i = parent;
        }
    }

    private void swap(int i, int j) {
        T t = (T) heap[i];
        heap[i] = heap[j];
        heap[j] = t;
    }

    private void resize() {
        int newCapacity = (capacity == 0) ? 1 : capacity * 2;
        this.capacity = newCapacity;
        heap = Arrays.copyOf(heap, newCapacity);
    }

    public T poll() {
        if (isEmpty()) {
            throw new RuntimeException("heap is empty");
        }
        T res = (T) heap[0];
        heap[0] = heap[--size];
        heap[size] = null;
        siftDown(0);
        return res;
    }

    private void siftDown(int i) {
        while (i * 2 + 1 < size) {
            int leftChild = i * 2 + 1, rightChild = i * 2 + 2;
            int maxChild = leftChild;
            if (rightChild < size && comparator.compare((T) heap[rightChild], (T) heap[leftChild]) >= 0) {
                maxChild = rightChild;
            }
            if (comparator.compare((T) heap[i], (T) heap[maxChild]) >= 0) {
                break;
            }
            swap(maxChild, i);
            i = maxChild;
        }
    }
}
