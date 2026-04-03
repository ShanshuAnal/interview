package com.nowcoder.community.code;

import java.util.*;

/**
 * @Author: 19599
 * @Date: 2025/6/16 19:19
 * @Description:
 */
public class nowercode {
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        int n = in.nextInt();
        for (int i = 0; i < n; i++) {
            List<Integer> alice = new ArrayList<>();
            List<Integer> bob = new ArrayList<>();

            // todo：这边不用存，直接边读边比
            for (int j = 0; j < 26; j++) {
                alice.add(in.nextInt());
            }
            for (int j = 0; j < 26; j++) {
                bob.add(in.nextInt());
            }

            int aliceNum = 0, bobNum = 0;
            Set<Integer> set = new HashSet<>();
            Deque<Integer> stack = new LinkedList<>();

            for (int j = 0; j < 26; j++) {
                int aliceCard = alice.get(j);
                int bobCard = bob.get(j);

                if (set.contains(aliceCard)) {
                    int len = 1;
                    while (!stack.isEmpty()) {
                        int top = stack.pop();
                        len++;
                        set.remove(top);
                        if (aliceCard == top) {
                            break;
                        }
                    }
                    aliceNum += len;
                } else {
                    set.add(aliceCard);
                    stack.push(aliceCard);
                }

                if (set.contains(bobCard)) {
                    int len = 1;
                    while (!stack.isEmpty()) {
                        int top = stack.pop();
                        len++;
                        set.remove(top);
                        if (bobCard == top) {
                            break;
                        }
                    }
                    bobNum += len;
                } else {
                    set.add(bobCard);
                    stack.push(bobCard);
                }
            }

            if (aliceNum == bobNum) {
                System.out.println("Draw");
            } else if (aliceNum > bobNum) {
                System.out.println("Alice");
            } else {
                System.out.println("Bob");
            }
        }
    }
}
