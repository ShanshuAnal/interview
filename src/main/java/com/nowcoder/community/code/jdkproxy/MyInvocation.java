package com.nowcoder.community.code.jdkproxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * @Author: 19599
 * @Date: 2025/9/6 4:54
 * @Description:
 */
public class MyInvocation implements InvocationHandler {

    private final Object target;

    public MyInvocation(Object target) {
        this.target = target;
    }


    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        System.out.println("pre");
        Object res = method.invoke(target, args);
        System.out.println("post");
        return res;
    }
}
