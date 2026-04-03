package com.nowcoder.community.code;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.function.UnaryOperator;

/**
 * @Author: 19599
 * @Date: 2025/6/17 15:17
 * @Description:
 * 输入多少人，多少钱
 * 输出随机金额数
 */
public class Test {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int people = sc.nextInt();
        double money = sc.nextDouble();

        // 将金额转成分为单位，避免double的精度损失
        int totalMoney = (int) Math.round(money * 100);

        List<Double> list = new ArrayList<>();
        Random random = new Random();

        // 二倍均值法
        // 每次随机的最大值是人均的2倍，避免极端值
        while (people >= 1) {
            if (people == 1) {
                list.add(totalMoney / 100.0);
                break;
            }

            // 使用二倍均值法，保证不会出现过大和过小
            // 每次随机的最大值是当前人均的 2 倍，避免极端值
            // 如果是普通的随机数，那么是均匀分布，会过大或者过小
            // 它实际上是一个偏态均匀分布（右偏），低值多，高值少，大多数金额集中在0.5 - 1.5倍均值之间
            int max = (totalMoney / people) * 2;
            int value = 1 + random.nextInt(max);

            list.add(value / 100.0);
            people--;
            totalMoney -= value;
        }

        // 保证两位小数
        list.replaceAll(new UnaryOperator<Double>() {
            @Override
            public Double apply(Double val) {
                return BigDecimal.valueOf(val).setScale(2, BigDecimal.ROUND_UP).doubleValue();
            }
        });

        System.out.println(list);
        System.out.printf("总和：%.2f\n", list.stream().mapToDouble(Double::doubleValue).sum());
    }
}
