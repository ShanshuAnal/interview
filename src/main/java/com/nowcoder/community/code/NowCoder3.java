package com.nowcoder.community.code;

import java.util.*;

/**
 * @Author: 19599
 * @Date: 2025/6/16 19:54
 * @Description:
 */
public class NowCoder3 {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int loop = sc.nextInt();
        while (loop-- > 0) {
            int m = sc.nextInt();
            int n = sc.nextInt();
            int[][] martix = new int[m][n];
            for (int i = 0; i < m; i++) {
                for (int j = 0; j < n; j++) {
                    martix[i][j] = sc.nextInt();
                }
            }

            int[][] dp = new int[m][n];
            dp[0][0] = martix[0][0];
            for (int j = 1; j < n; j++) {
                dp[0][j] = dp[0][j - 1] & martix[0][j];
            }
            for (int i = 1; i < m; i++) {
                dp[i][0] = dp[i - 1][0] & martix[i][0];
            }
            for (int i = 1; i < m; i++) {
                for (int j = 1; j < n; j++) {
                    dp[i][j] = most1(dp[i - 1][j] & martix[i][j], dp[i][j - 1] & martix[i][j]);
                }
            }
            System.out.println(dp[m - 1][n - 1]);
        }
    }

    private static int most1(int l1, int l2) {
        int num1 = 0, num2 = 0;
        for (int i = 0; i < 32; i++) {
            num1 += (l1 >> i) & 1;
            num2 += (l2 >> i) & 1;
        }
        return num1 >= num2 ? l1 : l2;
    }
}
