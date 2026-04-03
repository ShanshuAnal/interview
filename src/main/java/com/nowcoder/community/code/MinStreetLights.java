package com.nowcoder.community.code;

import javax.validation.constraints.Min;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * @Author: 19599
 * @Date: 2025/9/7 19:25
 * @Description:
 */
public class MinStreetLights {
    public int minStreetLights(int len, int[] positions, int r) {
        if (len <= 0) {
            return 0;
        }
        if (positions == null || positions.length == 0) {
            return len == 0 ? 0 : -1;
        }

        int n = positions.length;
        int[][] intervals = new int[n][2];
        for (int i = 0; i < n; i++) {
            int left = Math.max(0, positions[i] - r);
            int right = positions[i] + r;
            intervals[i] = new int[]{left, right};
        }

        ArrayList<Object> list = new ArrayList<>();
        list.parallelStream().forEach(System.out::println);
        Arrays.sort(intervals, (a, b) -> Integer.compare(a[0], b[0]));

        int res = 0;
        int currentEnd = 0;
        int index = 0;
        while (currentEnd < len) {
            int maxEnd = currentEnd;
            while (index < n && intervals[index][0] <= currentEnd) {
                maxEnd = Math.max(maxEnd, intervals[index][1]);
                index++;
            }

            res++;
            currentEnd = maxEnd;
            if (currentEnd >= len) {
                break;
            }
        }
        return currentEnd >= len ? res : -1;
    }

    public static void main(String[] args) {
        // 测试用例1：常规情况
        int L1 = 10;
        int[] positions1 = {1, 3, 5, 7, 9};
        int k1 = 2;
        System.out.println("测试用例1: " + new MinStreetLights().minStreetLights(L1, positions1, k1));  // 预期输出: 3

        // 测试用例2：刚好覆盖
        int L2 = 5;
        int[] positions2 = {2};
        int k2 = 3;
        System.out.println("测试用例2: " + new MinStreetLights().minStreetLights(L2, positions2, k2));  // 预期输出: 1

        // 测试用例3：无法覆盖（存在缺口）
        int L3 = 10;
        int[] positions3 = {1, 5};
        int k3 = 2;
        System.out.println("测试用例3: " + new MinStreetLights().minStreetLights(L3, positions3, k3));  // 预期输出: -1

        // 测试用例4：空路面
        int L4 = 0;
        int[] positions4 = {};
        int k4 = 1;
        System.out.println("测试用例4: " + new MinStreetLights().minStreetLights(L4, positions4, k4));  // 预期输出: 0

        // 测试用例5：单个路灯覆盖全部
        int L5 = 8;
        int[] positions5 = {4};
        int k5 = 4;
        System.out.println("测试用例5: " + new MinStreetLights().minStreetLights(L5, positions5, k5));  // 预期输出: 1
    }
}
