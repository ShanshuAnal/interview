package com.nowcoder.community.cache;

import com.alibaba.fastjson.JSON;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author 19599
 */
@Component
@Slf4j
public class LarkShopConfigBusiness {
    /**
     * Redis 缓存 key 前缀
     */
    private static final String CACHE_KEY_PREFIX = "lark_shop:";

    /**
     * Redis 分布式锁 key 前缀
     */
    private static final String LOCK_KEY_PREFIX = "lock:lark_shop:";

    /**
     * Redis 缓存基础 TTL（秒）
     */
    private static final int BASE_TTL = 600;

    /**
     * Redis 缓存 TTL 随机抖动范围（秒），防雪崩
     */
    private static final int TTL_JITTER = 120;

    /**
     * 空对象缓存 TTL（秒），防穿透
     */
    private static final int NULL_TTL = 60;

    /**
     * 分布式锁超时时间（秒）
     */
    private static final int LOCK_EXPIRE_SECONDS = 10;

    /**
     * 未抢到锁时每次自旋等待时间（ms）
     */
    private static final int SPIN_SLEEP_MS = 30;

    /**
     * 未抢到锁时最大自旋次数，总等待窗口 = 30ms * 3 = 90ms
     */
    private static final int MAX_SPIN_TIMES = 3;

    /**
     * 高敏感字段不进本地 Caffeine，只走 Redis
     * 这些字段一旦变更要求快速生效，不能容忍 30 秒本地缓存脏数据
     */
    private static final Set<String> HIGH_SENSITIVE_FIELDS = new HashSet<>(Arrays.asList(
            "isOnline", "apiKey", "secret", "popCollectMethod", "popRefundMethod"
    ));

    // ─────────────────────────────────────────────
    // 依赖
    // ─────────────────────────────────────────────

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private LarkShopConfigMapper larkShopConfigMapper;

    /**
     * 本地二级缓存 Caffeine
     * TTL = 30秒，Pub/Sub 广播失效 + TTL 兜底双重保障
     * 注意：高敏感字段对应的 storeCode 不存入此缓存（见 shouldUseLocalCache 方法说明）
     * 实际项目中如需按字段粒度控制，可拆分为两个 key 体系或存储时过滤敏感字段
     */
    private Cache<Integer, LarkShopConfig> localCache;

    /**
     * 限流器：Guava RateLimiter，令牌桶算法
     * 每秒最多放行 500 个请求进入缓存查询链路
     * 超出限流的请求直接返回降级默认值，保护 DB
     */
    private RateLimiter rateLimiter;

    /**
     * 版本比对写回 Lua 脚本
     */
    private DefaultRedisScript<Long> versionCompareSetScript;

    /**
     * 释放锁 Lua 脚本
     */
    private DefaultRedisScript<Long> releaseLockScript;

    @PostConstruct
    public void init() {
        // 本地缓存，TTL 30秒，最大缓存 1000 个店铺配置
        this.localCache = Caffeine.newBuilder()
                .expireAfterWrite(30, TimeUnit.SECONDS)
                .maximumSize(1000)
                .build();

        // 限流器，每秒 500 个请求
        this.rateLimiter = RateLimiter.create(500);

        // 版本比对写回脚本
        this.versionCompareSetScript = new DefaultRedisScript<>();
        this.versionCompareSetScript.setScriptText(RedisLuaScripts.VERSION_COMPARE_SET);
        this.versionCompareSetScript.setResultType(Long.class);

        // 释放锁脚本
        this.releaseLockScript = new DefaultRedisScript<>();
        this.releaseLockScript.setScriptText(RedisLuaScripts.RELEASE_LOCK);
        this.releaseLockScript.setResultType(Long.class);
    }

    // ─────────────────────────────────────────────
    // 公共方法
    // ─────────────────────────────────────────────

    /**
     * 获取店铺配置（二级缓存 + 防击穿 + 限流降级）
     * <p>
     * 查询顺序：本地 Caffeine → Redis → DB
     *
     * @param storeCode 店铺编码
     * @return 店铺配置，降级时返回空 LarkShopConfig（isOnline=0 保守默认值）
     */
    public LarkShopConfig getShopConfig(Integer storeCode) {
        // ① 限流检查，tryAcquire 非阻塞，拿不到令牌直接降级
        if (!rateLimiter.tryAcquire(100, TimeUnit.MILLISECONDS)) {
            log.warn("[LarkShopConfig] 限流触发，storeCode={}", storeCode);
            return buildFallbackConfig(storeCode);
        }
        // ② 查本地 Caffeine（纳秒级）
        LarkShopConfig localResult = localCache.getIfPresent(storeCode);
        if (localResult != null) {
            return isNullCache(localResult) ? null : localResult;
        }
        // ③ 查 Redis（1~2ms）
        LarkShopConfig redisResult = getFromRedis(storeCode);
        if (redisResult != null) {
            // 回填本地缓存，putIfAbsent 语义防止覆盖其他线程已写入的新数据
            localCache.get(storeCode, k -> redisResult);
            return isNullCache(redisResult) ? null : redisResult;
        }
        // ④ Redis 未命中，SETNX 抢分布式锁，防击穿
        return loadWithLock(storeCode);
    }

    /**
     * 失效本地缓存（由 Pub/Sub Subscriber 调用）
     */
    public void invalidateLocalCache(Integer storeCode) {
        localCache.invalidate(storeCode);
        log.info("[LarkShopConfig] 本地缓存已失效，storeCode={}", storeCode);
    }

    /**
     * 加分布式锁后查 DB 并写回缓存
     * 未抢到锁的线程自旋等待，最多 3 次（总等待 90ms）
     * 90ms 内仍未命中缓存，降级查 DB，只有 DB 也失败才返回保守默认值
     */
    private LarkShopConfig loadWithLock(Integer storeCode) {
        String lockKey = LOCK_KEY_PREFIX + storeCode;
        String lockValue = UUID.randomUUID().toString();
        // 尝试 SETNX 抢锁
        Boolean locked = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, lockValue, LOCK_EXPIRE_SECONDS, TimeUnit.SECONDS);
        if (Boolean.TRUE.equals(locked)) {
            // 抢到锁
            try {
                // 双重检查，防止其他线程刚完成写回
                LarkShopConfig doubleCheck = getFromRedis(storeCode);
                if (doubleCheck != null) {
                    localCache.get(storeCode, k -> doubleCheck);
                    return isNullCache(doubleCheck) ? null : doubleCheck;
                }
                // 查 DB
                LarkShopConfig config = larkShopConfigMapper.queryByStoreCode(storeCode);
                // 写回缓存
                if (config != null) {
                    writeToCache(storeCode, config);
                } else {
                    // 防穿透：写入空对象，TTL 短一点
                    writeNullCache(storeCode);
                }
                return config;
            } finally {
                // Lua 脚本原子释放锁，防止误删其他线程的锁
                releaseLock(lockKey, lockValue);
            }
        } else {
            // 未抢到锁，自旋等待
            return spinWait(storeCode);
        }
    }

    /**
     * 自旋等待
     * 每次 sleep(30ms) 后查缓存，最多 3 次（总等待窗口 90ms）
     * 耗尽后仍未命中，说明持锁线程查询较慢，但 DB 本身未必有问题
     * 此时降级查 DB 返回真实数据，只查不写回缓存（绕过锁直接写回可能引发并发写入问题，
     * 由持锁线程负责完成正常写回流程）
     * 只有 DB 也查询失败时，才返回保守默认值
     */
    private LarkShopConfig spinWait(Integer storeCode) {
        for (int i = 0; i < MAX_SPIN_TIMES; i++) {
            try {
                Thread.sleep(SPIN_SLEEP_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("[LarkShopConfig] 自旋等待被中断，storeCode={}", storeCode);
                break;
            }
            // 每次醒来先查本地缓存，再查 Redis
            LarkShopConfig localResult = localCache.getIfPresent(storeCode);
            if (localResult != null) {
                return isNullCache(localResult) ? null : localResult;
            }
            LarkShopConfig redisResult = getFromRedis(storeCode);
            if (redisResult != null) {
                localCache.get(storeCode, k -> redisResult);
                return isNullCache(redisResult) ? null : redisResult;
            }
        }
        // 自旋耗尽仍未命中，降级查 DB
        // 锁等待超时只说明持锁线程较慢，不代表 DB 有问题，不应误杀正常请求
        log.warn("[LarkShopConfig] 自旋耗尽仍未命中缓存，降级查 DB，storeCode={}", storeCode);
        try {
            LarkShopConfig config = larkShopConfigMapper.queryByStoreCode(storeCode);
            if (config != null) {
                return config;
            }
            // DB 返回空，正常情况（该 storeCode 不存在）
            return null;
        } catch (Exception e) {
            // DB 也查询失败，才返回保守默认值
            log.error("[LarkShopConfig] 降级查 DB 失败，返回保守默认值，storeCode={}，error={}",
                    storeCode, e.getMessage(), e);
            return buildFallbackConfig(storeCode);
        }
    }

    /**
     * 从 Redis 获取配置
     * Redis 中存储的是 CacheWrapper JSON，包含 version 和 data
     */
    private LarkShopConfig getFromRedis(Integer storeCode) {
        String cacheKey = CACHE_KEY_PREFIX + storeCode;
        String json = redisTemplate.opsForValue().get(cacheKey);
        if (json == null) {
            return null;
        }
        CacheWrapper wrapper = JSON.parseObject(json, CacheWrapper.class);
        if (wrapper == null || wrapper.getData() == null) {
            return null;
        }
        return JSON.parseObject(JSON.toJSONString(wrapper.getData()), LarkShopConfig.class);
    }

    /**
     * 写回 Redis + 本地 Caffeine
     * Redis 写回使用版本比对 Lua 脚本，防止老数据覆盖新数据
     */
    private void writeToCache(Integer storeCode, LarkShopConfig config) {
        String cacheKey = CACHE_KEY_PREFIX + storeCode;
        // 版本号取 updateTime 毫秒时间戳
        long version = config.getUpdateTime() != null
                ? config.getUpdateTime().getTime()
                : System.currentTimeMillis();
        CacheWrapper<LarkShopConfig> wrapper = new CacheWrapper<>(version, config);
        String json = JSON.toJSONString(wrapper);

        // TTL 加随机抖动防雪崩
        int ttl = BASE_TTL + new Random().nextInt(TTL_JITTER);
        // Lua 脚本原子写回，版本比对防老数据覆盖
        Long result = redisTemplate.execute(
                versionCompareSetScript,
                Collections.singletonList(cacheKey),
                json,
                String.valueOf(version),
                String.valueOf(ttl)
        );

        if (result != null && result == 1L) {
            // Redis 写回成功，putIfAbsent 写本地缓存
            localCache.get(storeCode, k -> config);
            log.debug("[LarkShopConfig] 缓存写回成功，storeCode={}，version={}", storeCode, version);
        } else {
            // 版本比对失败，Redis 中已有更新的数据，从 Redis 重新拉取回填本地缓存
            log.debug("[LarkShopConfig] 版本比对失败，Redis 中已有更新数据，storeCode={}", storeCode);
            LarkShopConfig latestFromRedis = getFromRedis(storeCode);
            if (latestFromRedis != null) {
                localCache.get(storeCode, k -> latestFromRedis);
            }
        }
    }

    /**
     * 写入空对象缓存（防穿透）
     */
    private void writeNullCache(Integer storeCode) {
        String cacheKey = CACHE_KEY_PREFIX + storeCode;
        // 空对象：isOnline=0，storeCode 填入方便识别
        LarkShopConfig nullConfig = buildFallbackConfig(storeCode);
        nullConfig.setDelFlag(-1); // 特殊标记，区分真实空对象和正常配置
        CacheWrapper<LarkShopConfig> wrapper = new CacheWrapper<>(0L, nullConfig);
        redisTemplate.opsForValue().set(cacheKey, JSON.toJSONString(wrapper), NULL_TTL, TimeUnit.SECONDS);
        localCache.get(storeCode, k -> nullConfig);
    }

    /**
     * 释放分布式锁（Lua 脚本原子执行）
     */
    private void releaseLock(String lockKey, String lockValue) {
        try {
            redisTemplate.execute(
                    releaseLockScript,
                    Collections.singletonList(lockKey),
                    lockValue
            );
        } catch (Exception e) {
            log.error("[LarkShopConfig] 释放锁失败，lockKey={}，error={}", lockKey, e.getMessage());
        }
    }

    /**
     * 判断是否是空对象缓存（防穿透写入的占位对象）
     */
    private boolean isNullCache(LarkShopConfig config) {
        return config.getDelFlag() != null && config.getDelFlag() == -1;
    }

    /**
     * 构建降级保守默认值
     * isOnline = 0，保守地认为店铺不可用，避免因缓存问题导致资损
     */
    private LarkShopConfig buildFallbackConfig(Integer storeCode) {
        LarkShopConfig fallback = new LarkShopConfig();
        fallback.setStoreCode(storeCode);
        fallback.setIsOnline(0);
        return fallback;
    }
}
