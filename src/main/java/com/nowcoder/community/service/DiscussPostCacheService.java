package com.nowcoder.community.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.nowcoder.community.config.CacheConfig;
import com.nowcoder.community.dao.DiscussPostMapper;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.util.RedisKeyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.*;

@Service
public class DiscussPostCacheService {

    private static final Logger logger = LoggerFactory.getLogger(DiscussPostCacheService.class);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private DiscussPostMapper discussPostMapper;

    @Autowired
    private CacheConfig cacheConfig;

    // 本地缓存
    private Cache<Integer, DiscussPost> postDetailCache;
    private Cache<String, List<DiscussPost>> postListCache;
    private Cache<Integer, Integer> postCountCache;

    @PostConstruct
    public void init() {
        postDetailCache = Caffeine.newBuilder()
                .maximumSize(cacheConfig.getLocal().getPost().getDetailMaxSize())
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .build();

        postListCache = Caffeine.newBuilder()
                .maximumSize(cacheConfig.getLocal().getPost().getListMaxSize())
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .build();

        postCountCache = Caffeine.newBuilder()
                .maximumSize(cacheConfig.getLocal().getPost().getCountMaxSize())
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .build();
    }

    public DiscussPost getPostDetail(int postId) {
        // 1. 查询本地缓存
        DiscussPost post = postDetailCache.getIfPresent(postId);
        if (post != null) {
            logger.debug("从本地缓存获取帖子详情: {}", postId);
            return post;
        }
        Callable<Integer> callable = new Callable<>() {
            @Override
            public Integer call() throws Exception {
                return 1;
            }
        };

        ExecutorService executor = Executors.newFixedThreadPool(10);
        CompletableFuture<Void> future1 = CompletableFuture.runAsync(() -> {
            System.out.println(1);
        }, executor);



        // 2. 查询Redis缓存
        String redisKey = RedisKeyUtil.getPostDetailKey(postId);
        post = (DiscussPost) redisTemplate.opsForValue().get(redisKey);
        if (post != null) {
            logger.debug("从Redis缓存获取帖子详情: {}", postId);
            postDetailCache.put(postId, post);
            return post;
        }

        // 3. 查询数据库
        logger.debug("从数据库获取帖子详情: {}", postId);
        post = discussPostMapper.selectDiscussPostById(postId);
        if (post != null) {
            redisTemplate.opsForValue().set(redisKey, post, 
                    RedisKeyUtil.POST_DETAIL_CACHE_EXPIRE_SECONDS, TimeUnit.SECONDS);
            postDetailCache.put(postId, post);
        }

        return post;
    }

    public List<DiscussPost> getPostList(int userId, int offset, int limit, int orderMode) {
        String cacheKey = userId + ":" + orderMode + ":" + offset + ":" + limit;
        
        // 1. 查询本地缓存
        List<DiscussPost> posts = postListCache.getIfPresent(cacheKey);
        if (posts != null) {
            logger.debug("从本地缓存获取帖子列表: {}", cacheKey);
            return posts;
        }

        // 2. 查询Redis缓存
        String redisKey = RedisKeyUtil.getPostListKey(userId, orderMode, offset, limit);
        posts = (List<DiscussPost>) redisTemplate.opsForValue().get(redisKey);
        if (posts != null) {
            logger.debug("从Redis缓存获取帖子列表: {}", cacheKey);
            postListCache.put(cacheKey, posts);
            return posts;
        }

        // 3. 查询数据库
        logger.debug("从数据库获取帖子列表: {}", cacheKey);
        posts = discussPostMapper.selectDiscussPosts(userId, offset, limit, orderMode);
        if (posts != null && !posts.isEmpty()) {
            redisTemplate.opsForValue().set(redisKey, posts, 
                    RedisKeyUtil.POST_LIST_CACHE_EXPIRE_SECONDS, TimeUnit.SECONDS);
            postListCache.put(cacheKey, posts);
        }

        return posts;
    }

    public int getPostCount(int userId) {
        // 1. 查询本地缓存
        Integer count = postCountCache.getIfPresent(userId);
        if (count != null) {
            logger.debug("从本地缓存获取帖子总数: {}", userId);
            return count;
        }

        // 2. 查询Redis缓存
        String redisKey = RedisKeyUtil.getPostCountKey(userId);
        count = (Integer) redisTemplate.opsForValue().get(redisKey);
        if (count != null) {
            logger.debug("从Redis缓存获取帖子总数: {}", userId);
            postCountCache.put(userId, count);
            return count;
        }

        // 3. 查询数据库
        logger.debug("从数据库获取帖子总数: {}", userId);
        count = discussPostMapper.selectDiscussPostRows(userId);
        redisTemplate.opsForValue().set(redisKey, count, 
                RedisKeyUtil.POST_CACHE_EXPIRE_SECONDS, TimeUnit.SECONDS);
        postCountCache.put(userId, count);

        return count;
    }

    public void evictPostDetail(int postId) {
        postDetailCache.invalidate(postId);
        String redisKey = RedisKeyUtil.getPostDetailKey(postId);
        redisTemplate.delete(redisKey);
        logger.debug("清除帖子详情缓存: {}", postId);
    }

    public void evictPostList(int userId, int orderMode) {
        postListCache.asMap().keySet().removeIf(key -> 
                key.startsWith(userId + ":" + orderMode + ":"));
        logger.debug("清除帖子列表缓存: userId={}, orderMode={}", userId, orderMode);
    }

    public void evictPostCount(int userId) {
        postCountCache.invalidate(userId);
        String redisKey = RedisKeyUtil.getPostCountKey(userId);
        redisTemplate.delete(redisKey);
        logger.debug("清除帖子总数缓存: {}", userId);
    }
} 