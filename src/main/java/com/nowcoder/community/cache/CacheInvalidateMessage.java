package com.nowcoder.community.cache;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 缓存失效 MQ 消息体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CacheInvalidateMessage {

    /** Redis cache key，例如 lark_shop:1001 */
    private String cacheKey;

    /** 店铺编码，用于广播失效本地 Caffeine */
    private Integer storeCode;
}
