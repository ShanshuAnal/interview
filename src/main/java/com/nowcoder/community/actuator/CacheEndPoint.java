package com.nowcoder.community.actuator;

import com.nowcoder.community.service.DiscussPostCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 缓存监控端点
 * 提供缓存状态查看和手动清除功能
 */
@Component
@Endpoint(id = "cache")
public class CacheEndPoint {

    @Autowired
    private DiscussPostCacheService cacheService;

    /**
     * 查看缓存状态
     */
    @ReadOperation
    public Map<String, Object> getCacheStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("message", "帖子缓存服务运行正常");
        status.put("timestamp", System.currentTimeMillis());
        status.put("cacheService", "DiscussPostCacheService");
        return status;
    }

    /**
     * 清除指定帖子详情缓存
     */
    @WriteOperation
    public Map<String, Object> evictPostDetail(int postId) {
        Map<String, Object> result = new HashMap<>();
        try {
            cacheService.evictPostDetail(postId);
            result.put("success", true);
            result.put("message", "成功清除帖子详情缓存: " + postId);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "清除帖子详情缓存失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 清除指定用户的帖子列表缓存
     */
    @WriteOperation
    public Map<String, Object> evictPostList(int userId, int orderMode) {
        Map<String, Object> result = new HashMap<>();
        try {
            cacheService.evictPostList(userId, orderMode);
            result.put("success", true);
            result.put("message", "成功清除帖子列表缓存: userId=" + userId + ", orderMode=" + orderMode);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "清除帖子列表缓存失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 清除指定用户的帖子总数缓存
     */
    @WriteOperation
    public Map<String, Object> evictPostCount(int userId) {
        Map<String, Object> result = new HashMap<>();
        try {
            cacheService.evictPostCount(userId);
            result.put("success", true);
            result.put("message", "成功清除帖子总数缓存: " + userId);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "清除帖子总数缓存失败: " + e.getMessage());
        }
        return result;
    }
} 