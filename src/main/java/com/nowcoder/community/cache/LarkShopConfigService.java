package com.nowcoder.community.cache;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class LarkShopConfigService {

    /**
     * Redis 缓存 key 前缀
     */
    private static final String CACHE_KEY_PREFIX = "lark_shop:";

    /**
     * Pub/Sub 广播 channel，各实例订阅此 channel 失效本地 Caffeine
     */
    private static final String PUBSUB_CHANNEL = "cache:invalidate:lark_shop";

    /**
     * MQ Topic
     */
    private static final String MQ_TOPIC = "cache-invalidate-topic";

    @Autowired
    private LarkShopConfigMapper larkShopConfigMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    /**
     * 更新店铺配置
     * 写链路：先更新 DB，再删 Redis，再广播失效本地缓存
     *
     * @param config 待更新的店铺配置
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateShopConfig(LarkShopConfig config) {
        // ① 更新 DB（事务内）
        larkShopConfigMapper.updateById(config);
        // ② 注册事务同步回调：确保只有在数据库真正 COMMIT 成功后才触发缓存清理
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                // 提交成功后，异步执行缓存失效逻辑，不阻塞主业务逻辑
                CompletableFuture.runAsync(() -> afterUpdateShopConfig(config));
            }
        });
    }

    /**
     * 事务提交后执行缓存失效
     * 注意：此方法由 AOP 或 TransactionSynchronizationManager 在事务提交后调用
     * 这里简化为直接调用，实际项目可结合 @TransactionalEventListener 解耦
     */
    public void afterUpdateShopConfig(LarkShopConfig config) {
        String cacheKey = CACHE_KEY_PREFIX + config.getStoreCode();
        // ② DEL Redis
        boolean delSuccess = deleteRedisKey(cacheKey);
        if (delSuccess) {
            // ③ DEL 成功，Pub/Sub 广播通知各实例失效本地 Caffeine
            publishInvalidate(config.getStoreCode());
        } else {
            // ④ DEL 失败，投递 MQ 异步补偿，不在主线程重试，避免阻塞
            log.error("[LarkShopConfig] DEL Redis 失败，投递 MQ 补偿，cacheKey={}", cacheKey);
            sendToMQ(cacheKey, config.getStoreCode());
        }
    }

    /**
     * 删除 Redis key
     *
     * @return true=删除成功，false=删除失败
     */
    private boolean deleteRedisKey(String cacheKey) {
        try {
            redisTemplate.delete(cacheKey);
            log.info("[LarkShopConfig] DEL Redis 成功，cacheKey={}", cacheKey);
            return true;
        } catch (Exception e) {
            log.error("[LarkShopConfig] DEL Redis 异常，cacheKey={}，error={}", cacheKey, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Pub/Sub 广播缓存失效通知
     * 各实例的 LarkCacheSubscriber 收到消息后 invalidate 本地 Caffeine
     * <p>
     * 注意：Pub/Sub 是 Fire-and-Forget，不保证 100% 送达
     * 漏收的实例依赖本地 Caffeine TTL（30秒）兜底
     * 高敏感字段不进本地缓存，不受此影响
     */
    private void publishInvalidate(Integer storeCode) {
        try {
            redisTemplate.convertAndSend(PUBSUB_CHANNEL, String.valueOf(storeCode));
            log.info("[LarkShopConfig] Pub/Sub 广播成功，storeCode={}", storeCode);
        } catch (Exception e) {
            // 广播失败不做补偿，依赖本地 Caffeine TTL 兜底
            log.warn("[LarkShopConfig] Pub/Sub 广播失败，storeCode={}，依赖 TTL 兜底，error={}",
                    storeCode, e.getMessage());
        }
    }

    /**
     * 投递 MQ 消息，异步补偿 DEL Redis
     * Consumer 消费成功后会重新执行 DEL + Publish 广播
     */
    private void sendToMQ(String cacheKey, Integer storeCode) {
        try {
            CacheInvalidateMessage message = new CacheInvalidateMessage(cacheKey, storeCode);
            rocketMQTemplate.send(MQ_TOPIC,
                    MessageBuilder.withPayload(JSON.toJSONString(message)).build());
            log.info("[LarkShopConfig] MQ 投递成功，cacheKey={}", cacheKey);
        } catch (Exception e) {
            // MQ 投递失败，打 ERROR 日志 + 告警，依赖 Redis TTL 最终兜底（10分钟内自然过期）
            log.error("[LarkShopConfig] MQ 投递失败，cacheKey={}，需人工介入，error={}",
                    cacheKey, e.getMessage(), e);
            // 实际项目接入告警平台（钉钉/Sentry等）
        }
    }
}
