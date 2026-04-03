package com.nowcoder.community.cache;

/**
 * Redis Lua 脚本常量
 */
public class RedisLuaScripts {

    /**
     * 释放分布式锁
     * 原子性保证：GET + DEL，防止误删其他线程持有的锁
     * KEYS[1] = lockKey, ARGV[1] = lockValue (UUID)
     */
    public static final String RELEASE_LOCK =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "    return redis.call('del', KEYS[1]) " +
            "else " +
            "    return 0 " +
            "end";

    /**
     * 版本号比对写回
     * 只有回写数据的 version >= Redis 内已有数据的 version（或 Redis 为空）才允许写入
     * 防止读写并发场景下老数据覆盖新数据
     *
     * KEYS[1] = cacheKey
     * ARGV[1] = 新数据 JSON（含 version 字段）
     * ARGV[2] = 新数据的 version（updateTime 毫秒时间戳）
     * ARGV[3] = TTL 秒数
     */
    public static final String VERSION_COMPARE_SET =
            "local existing = redis.call('get', KEYS[1]) " +
            "if existing then " +
            "    local existingVersion = tonumber(cjson.decode(existing)['version']) " +
            "    if tonumber(ARGV[2]) < existingVersion then " +
            "        return 0 " +  // 回写数据版本更旧，拒绝写入
            "    end " +
            "end " +
            "redis.call('setex', KEYS[1], ARGV[3], ARGV[1]) " +
            "return 1";
}
