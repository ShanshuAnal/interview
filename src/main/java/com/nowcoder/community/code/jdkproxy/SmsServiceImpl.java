package com.nowcoder.community.code.jdkproxy;

/**
 * @Author: 19599
 * @Date: 2025/9/6 4:54
 * @Description:
 */
public class SmsServiceImpl implements SmsService {
    @Override
    public String send(String msg) {
        System.out.println(msg);
        return msg;
    }
}
