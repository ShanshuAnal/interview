package com.nowcoder.community.code;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * @Author: 19599
 * @Date: 2025/7/9 17:23
 * @Description:
 */
public class VowelCaseSwapper {
    public String swapVowelCase(String s) {
        if (s == null || s.isEmpty()) {
            return null;
        }
        Set<Character> set = new HashSet<>(Arrays.asList('a', 'e', 'i', 'o', 'u'));
        Iterator<Character> it = set.iterator();
        for (Character ch : set) {
            System.out.println(ch);
        }

        StringBuilder sb = new StringBuilder();
        for (char ch : s.toCharArray()) {
            if (set.contains(ch)) {
                if (Character.isUpperCase(ch)) {
                    sb.append(Character.toLowerCase(ch));
                } else {
                    sb.append(Character.toUpperCase(ch));
                }
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        System.out.println(new VowelCaseSwapper().swapVowelCase("aeiou JHlhjkhHJK"));
    }
}
