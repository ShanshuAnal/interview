package com.nowcoder.community.cache;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 缓存失效 MQ Consumer
 * <p>
 * 职责：消费 DEL Redis 失败后投递的补偿消息
 * 消费成功后重新执行 DEL Redis + Pub/Sub 广播
 * <p>
 * 重试策略：由 RocketMQ Broker 控制，消费失败 NACK 后按退避策略重投
 * 默认重试 16 次（10s→30s→1min→2min→3min→4min→5min→6min→7min→8min→9min→10min→20min→30min→1h→2h）
 * 超出重试次数后进入死信队列（%DLQ%consumer-group），触发告警人工介入
 */
@Component
@Slf4j
@RocketMQMessageListener(
        topic = "cache-invalidate-topic",
        consumerGroup = "lark-cache-invalidate-consumer-group"
)
public class LarkCacheInvalidateConsumer implements RocketMQListener<String> {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private LarkShopConfigBusiness larkShopConfigBusiness;

    /**
     * Pub/Sub 广播 channel
     */
    private static final String PUBSUB_CHANNEL = "cache:invalidate:lark_shop";

    @Override
    public void onMessage(String messageBody) {
        CacheInvalidateMessage message = JSON.parseObject(messageBody, CacheInvalidateMessage.class);
        if (message == null || message.getCacheKey() == null) {
            log.error("[LarkCacheInvalidateConsumer] 消息体解析失败，messageBody={}", messageBody);
            // 消息格式错误，直接 ACK 不重试，避免死循环
            return;
        }
        String cacheKey = message.getCacheKey();
        Integer storeCode = message.getStoreCode();

        // DEL Redis，抛出异常则 RocketMQ 自动 NACK，触发重试
        redisTemplate.delete(cacheKey);
        log.info("[LarkCacheInvalidateConsumer] DEL Redis 成功，cacheKey={}", cacheKey);

        // DEL 成功后，补发 Pub/Sub 广播通知各实例失效本地 Caffeine
        // 广播失败不影响 ACK，依赖本地 Caffeine TTL 兜底
        try {
            redisTemplate.convertAndSend(PUBSUB_CHANNEL, String.valueOf(storeCode));
            log.info("[LarkCacheInvalidateConsumer] Pub/Sub 广播成功，storeCode={}", storeCode);
        } catch (Exception e) {
            log.warn("[LarkCacheInvalidateConsumer] Pub/Sub 广播失败，storeCode={}，依赖 TTL 兜底，error={}",
                    storeCode, e.getMessage());
        }
        // 正常返回 = ACK
        log.info("[LarkCacheInvalidateConsumer] 消费成功，cacheKey={}", cacheKey);
    }
}
