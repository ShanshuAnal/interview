package com.nowcoder.community.leetcode;
/**
 * @Author: 19599
 * @Date: 2026/4/6 0:49
 * @Description:
 */
public class CanConstruct {

    public static void main(String[] args) {
        System.out.println(canConstruct("cmcm", "cm"));
        System.out.println(canConstruct("ccmm", "cm"));
        System.out.println(canConstruct("aaabbcbcc", "abc"));
        System.out.println(canConstruct("ababccabc", "abc"));
    }

    public static boolean canConstruct(String target, String source) {
        int n = target.length(), m = source.length();
        if (n % m != 0) {
            return false;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            char ch = target.charAt(i);
            sb.append(ch);
            if (sb.length() >= m) {
                boolean match = true;
                for (int j = 0; j < m; j++) {
                    if (sb.charAt(sb.length() -m + j) != source.charAt(j)) {
                        match = false;
                        break;
                    }
                }
                if (match) {
                    sb.setLength(sb.length() - m);
                }
            }
        }
        return sb.length() == 0;
    }
}
