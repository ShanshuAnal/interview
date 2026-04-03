package com.nowcoder.community.code;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 基于 StringRedisTemplate 的分布式锁工具类
 */
public class RedisDistributedLock {

    private final StringRedisTemplate stringRedisTemplate;

    // 解锁的 Lua 脚本（保证查询和删除的原子性）
    private static final String UNLOCK_SCRIPT = "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "return redis.call('del', KEYS[1]) " +
            "else " +
            "return 0 " +
            "end";

    public RedisDistributedLock(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 加锁方法
     * @param lockKey 锁的 key（如 "order:123:lock"）
     * @param expireTime 锁的过期时间（毫秒）
     * @return 加锁成功返回 uniqueValue（用于解锁），失败返回 null
     */
    public String tryLock(String lockKey, long expireTime) {
        // 生成唯一值（UUID），用于标识当前锁的持有者
        String uniqueValue = UUID.randomUUID().toString();

        // 执行 SET lockKey uniqueValue NX PX expireTime 命令
        // 参数说明：
        // - NX：仅当 key 不存在时才设置（对应 SetOption.ifAbsent()）
        // - PX：设置过期时间单位为毫秒
        Boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, uniqueValue, expireTime, TimeUnit.MILLISECONDS);

        // 若成功（success 为 true），返回 uniqueValue；否则返回 null
        return (success != null && success) ? uniqueValue : null;
    }

    /**
     * 解锁方法（必须传入加锁时返回的 uniqueValue）
     * @param lockKey 锁的 key
     * @param uniqueValue 加锁时的唯一值（用于校验是否为当前持有者）
     * @return 解锁成功返回 true，失败返回 false
     */
    public boolean unlock(String lockKey, String uniqueValue) {
        if (StringUtils.isEmpty(uniqueValue)) {
            return false; // 没有加锁，无需解锁
        }

        // 执行 Lua 脚本，保证 "查询值+删除锁" 的原子性
        RedisScript<Long> script = new DefaultRedisScript<>(UNLOCK_SCRIPT, Long.class);
        Long result = stringRedisTemplate.execute(
                script,
                Collections.singletonList(lockKey), // KEYS[1] = lockKey
                uniqueValue // ARGV[1] = uniqueValue
        );


        // 若 result 为 1，表示解锁成功；0 表示锁已被其他线程持有或已过期
        return result != null && result == 1;
    }
}
