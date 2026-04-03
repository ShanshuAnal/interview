package com.nowcoder.community.code.jdkproxy;

import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author: 19599
 * @Date: 2025/9/6 4:56
 * @Description:
 */
public class SmsServiceProxy {
    public static Object getProxy(Object target) {
        Map<Object, Object> objectObjectMap = Collections.synchronizedMap(new HashMap<>());
        return Proxy.newProxyInstance(target.getClass().getClassLoader(),
                target.getClass().getInterfaces(),
                new MyInvocation(target));
    }
}
