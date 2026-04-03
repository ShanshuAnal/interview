package com.nowcoder.community.code;

/**
 * @Author: 19599
 * @Date: 2025/6/28 0:34
 * @Description: 两数相减
 */
public class SubString {
    public String subString(String num1, String num2) {
        String sb = new String();
        if (isLess(num1, num2)) {
            sb = sub(num2, num1);
            if (sb.length() != 1 && sb.charAt(0) != '0') {
                sb = "-" + sb;
            }
        } else {
            sb = sub(num1, num2);
        }
        return sb;
    }
/*
*
* 895
* 816
*
* 079
* */
    private String sub(String num1, String num2) {
        StringBuilder sb = new StringBuilder();
        int borrow = 0;
        int len1 = num1.length() - 1, len2 = num2.length() - 1;
        while (len1 >= 0 || len2 >= 0) {
            int x = len1 >= 0 ? num1.charAt(len1--) - '0' : 0;
            int y = len2 >= 0 ? num2.charAt(len2--) - '0' : 0;

            int z = (x - borrow - y + 10) % 10;
            sb.append(z);

            borrow = (x - borrow - y) < 0 ? 1 : 0;
        }
        sb.reverse();
        int index = 0;
        for (; index < sb.length() - 1; index++) {
            if (sb.charAt(index) != '0') {
                break;
            }
        }
        return sb.substring(index);
    }

    private boolean isLess(String num1, String num2) {
        if (num1.length() == num2.length()) {
            return num1.compareTo(num2) < 0;
        } else {
            return num1.length() < num2.length();
        }
    }

    public static void main(String[] args) {
        System.out.println(new SubString().subString("895", "819"));
        System.out.println(new SubString().subString("895", "1819"));
        System.out.println(new SubString().subString("1819", "895"));
        System.out.println(new SubString().subString("1895", "1895"));
    }
}
