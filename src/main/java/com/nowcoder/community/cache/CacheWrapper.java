package com.nowcoder.community.cache;

import lombok.Data;

/**
 * Redis 缓存值包装类
 * 携带版本号（updateTime 毫秒时间戳），用于写回时的版本比对
 * 防止读写并发场景下老数据覆盖新数据
 */
@Data
public class CacheWrapper<T> {

    /**
     * 版本号，取 updateTime 的毫秒时间戳
     * 写回时通过 Lua 脚本比对，version 更旧的数据不允许写入
     */
    private long version;

    /**
     * 实际缓存数据
     */
    private T data;

    public CacheWrapper(long version, T data) {
        this.version = version;
        this.data = data;
    }
}
