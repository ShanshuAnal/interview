package com.nowcoder.community.code;

import java.util.*;

/**
 * @Author: 19599
 * @Date: 2025/6/16 19:41
 * @Description:
 */
public class NowCoder2 {
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        int n = in.nextInt();
        List<List<Integer>> list = new ArrayList<>();
        for (int i = 0; i <= n; i++) {
            list.add(new ArrayList<>());
        }
        for (int i = 0; i < n; i++) {
            int s = in.nextInt();
            int e = in.nextInt();
            list.get(s).add(e);
            list.get(e).add(s);
        }

        int[] minDistance = new int[n + 1];
        Arrays.fill(minDistance, n + 1);
        minDistance[1] = 0;

        Queue<Integer> que = new LinkedList<>();
        que.add(1);

        while (!que.isEmpty()) {
            int s = que.poll();
            List<Integer> dist = list.get(s);
            for (int e : dist) {
                if (minDistance[s] + 1 < minDistance[e]) {
                    minDistance[e] = minDistance[s] + 1;
                    que.offer(e);
                }
            }
        }
        for (int i = 1; i <= n; i++) {
            System.out.println(minDistance[i]);
        }
    }
}
