package com.nowcoder.community.code;

import java.util.Arrays;

/**
 * @Author: 19599
 * @Date: 2025/7/6 2:37
 * @Description:
 */
public class MergeSort {

    public void sortIntArray(int[] nums) {
        if (nums == null || nums.length == 1) {
            return;
        }
        int[] temp = new int[nums.length];
        mergeSortArrays(nums, 0, nums.length - 1, temp);
    }

    private void mergeSortArrays(int[] nums, int start, int end, int[] temp) {
        if (start >= end) {
            return;
        }
        // 1. 分解 (Divide)
        int mid = start + (end - start) / 2;
        // 递归地对左半部分进行排序
        mergeSortArrays(nums, start, mid, temp);
        // 递归地对右半部分进行排序
        mergeSortArrays(nums, mid + 1, end, temp);

        // 2. 合并 (Conquer)
        // 当左右两个子数组都已经有序后，将它们合并
        merge2Arrays(nums, start, mid, end, temp);
    }

    // 核心的“合并”方法，将 nums[start...mid] 和 nums[mid+1...end] 这两部分合并
    private void merge2Arrays(int[] nums, int start, int mid, int end, int[] temp) {
        // 指向左边子数组的起始位置
        int i = start;
        // 指向右边子数组的起始位置
        int j = mid + 1;
        // 指向临时数组 temp 的起始位置
        int t = 0;

        // (1) 比较左右两个子数组的元素，按从小到大的顺序填充到 temp 数组
        while (i <= mid && j <= end) {
            if (nums[i] <= nums[j]) {
                temp[t++] = nums[i++];
            } else {
                temp[t++] = nums[j++];
            }
        }

        // (2) 处理剩余的元素
        // 如果左边子数组还有剩余元素，则全部复制到 temp 数组
        while (i <= mid) {
            temp[t++] = nums[i++];
        }
        // 如果右边子数组还有剩余元素，则全部复制到 temp 数组
        while (j <= end) {
            temp[t++] = nums[j++];
        }

        // (3) 将 temp 数组中已排序的元素复制回原始数组 arr 的相应位置
        System.arraycopy(temp, 0, nums, start, end - start + 1);
    }

    public Node mergeLinkedList(Node head) {
        if (head == null || head.next == null) {
            return head;
        }
        Node slow = head, fast = head;
        while (fast.next != null) {
            fast = fast.next;
            if (fast.next == null) {
                break;
            }
            fast = fast.next;
            slow = slow.next;
        }
        fast = slow.next;
        slow.next = null;
        return merge2Lists(head, fast);
    }

    public Node mergeLinkedLists(Node[] nodes) {
        if (nodes == null || nodes.length == 0) {
            return null;
        }
        return mergeSortNodes(nodes, 0, nodes.length - 1);
    }

    private Node mergeSortNodes(Node[] nodes, int left, int right) {
        if (left == right) {
            return null;
        }
        int mid = left + (right - left) / 2;
        Node l1 = mergeSortNodes(nodes, left, mid);
        Node l2 = mergeSortNodes(nodes, mid + 1, right);
        return merge2Lists(l1, l2);
    }

    private Node merge2Lists(Node l1, Node l2) {
        Node h = new Node(-1, null), tail = h;
        while (l1 != null && l2 != null) {
            if (l1.val <= l2.val) {
                tail.next = l1;
                l1 = l1.next;
            } else {
                tail.next = l2;
                l2 = l2.next;
            }
            tail = tail.next;
        }
        tail.next = (l1 == null) ? l2 : l1;
        return h.next;
    }

    static class Node {
        int val;
        Node next;
        Node (int val, Node next) {
            this.val = val;
            this.next = next;
        }
    }

    public static void main(String[] args) {
        int[] nums = {1, 4, 7, 3, 7, 11, 6, 9, 44, 2};
        new MergeSort().sortIntArray(nums);
        System.out.println(Arrays.toString(nums));
    }
}