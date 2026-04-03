package com.nowcoder.community.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

/**
 * Redis Pub/Sub 订阅配置
 * <p>
 * 订阅 cache:invalidate:lark_shop channel
 * 收到广播后 invalidate 本地 Caffeine 对应 key
 * <p>
 * 注意：Pub/Sub 是 Fire-and-Forget，不保证 100% 送达
 * 实例重启期间、网络闪断时可能漏收消息
 * 漏收的实例依赖本地 Caffeine TTL（30秒）兜底
 */
@Configuration
@Slf4j
public class LarkCacheSubscriber {

    private static final String PUBSUB_CHANNEL = "cache:invalidate:lark_shop";

    @Autowired
    private LarkShopConfigBusiness larkShopConfigBusiness;

    /**
     * 消息监听容器
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        // 注册监听器，绑定 channel
        container.addMessageListener(
                messageListenerAdapter(),
                new PatternTopic(PUBSUB_CHANNEL)
        );

        return container;
    }

    /**
     * 消息监听适配器
     * 将 Redis 消息转发到 handleMessage 方法
     */
    @Bean
    public MessageListenerAdapter messageListenerAdapter() {
        return new MessageListenerAdapter(new CacheInvalidateHandler(), "handleMessage");
    }

    /**
     * 实际处理广播消息的内部类
     */
    public class CacheInvalidateHandler {

        /**
         * 处理缓存失效广播
         *
         * @param message 消息内容，即 storeCode 字符串
         */
        public void handleMessage(String message) {
            try {
                Integer storeCode = Integer.valueOf(message.trim());
                larkShopConfigBusiness.invalidateLocalCache(storeCode);
                log.info("[LarkCacheSubscriber] 收到广播，本地缓存已失效，storeCode={}", storeCode);
            } catch (NumberFormatException e) {
                log.error("[LarkCacheSubscriber] 广播消息格式错误，message={}，error={}", message, e.getMessage());
            }
        }
    }
}
