package com.nowcoder.community.service;

import com.nowcoder.community.dao.DiscussPostMapper;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.util.SensitiveFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.util.List;

/**
 * @author 19599
 */
@Service
public class DiscussPostService {

    private static final Logger logger = LoggerFactory.getLogger(DiscussPostService.class);

    @Autowired
    private DiscussPostMapper discussPostMapper;

    @Autowired
    private SensitiveFilter sensitiveFilter;

    @Autowired
    private DiscussPostCacheService cacheService;

    /**
     * 获取帖子列表（使用二级缓存）
     */
    public List<DiscussPost> findDiscussPosts(int userId, int offset, int limit, int orderMode) {
        return cacheService.getPostList(userId, offset, limit, orderMode);
    }

    /**
     * 获取帖子总数（使用二级缓存）
     */
    public int findDiscussPostRows(int userId) {
        return cacheService.getPostCount(userId);
    }

    /**
     * 添加帖子
     */
    public int addDiscussPost(DiscussPost post) {
        if (post == null) {
            throw new IllegalArgumentException("参数不能为空!");
        }

        // 转义HTML标记
        post.setTitle(HtmlUtils.htmlEscape(post.getTitle()));
        post.setContent(HtmlUtils.htmlEscape(post.getContent()));
        // 过滤敏感词
        post.setTitle(sensitiveFilter.filter(post.getTitle()));
        post.setContent(sensitiveFilter.filter(post.getContent()));

        int result = discussPostMapper.insertDiscussPost(post);
        
        // 清除相关缓存
        if (result > 0) {
            // 清除帖子列表缓存
            cacheService.evictPostList(0, 0); // 最新帖子
            cacheService.evictPostList(0, 1); // 热门帖子
            if (post.getUserId() != 0) {
                cacheService.evictPostList(post.getUserId(), 0);
                cacheService.evictPostList(post.getUserId(), 1);
            }
            // 清除帖子总数缓存
            cacheService.evictPostCount(0);
            if (post.getUserId() != 0) {
                cacheService.evictPostCount(post.getUserId());
            }
        }

        return result;
    }

    /**
     * 获取帖子详情（使用二级缓存）
     */
    public DiscussPost findDiscussPostById(int id) {
        return cacheService.getPostDetail(id);
    }

    /**
     * 更新评论数
     */
    public int updateCommentCount(int id, int commentCount) {
        int result = discussPostMapper.updateCommentCount(id, commentCount);
        
        // 清除相关缓存
        if (result > 0) {
            cacheService.evictPostDetail(id);
            // 清除帖子列表缓存（因为评论数变化可能影响排序）
            cacheService.evictPostList(0, 0);
            cacheService.evictPostList(0, 1);
        }
        
        return result;
    }

    /**
     * 更新帖子类型
     */
    public int updateType(int id, int type) {
        int result = discussPostMapper.updateType(id, type);
        
        // 清除相关缓存
        if (result > 0) {
            cacheService.evictPostDetail(id);
            cacheService.evictPostList(0, 0);
            cacheService.evictPostList(0, 1);
        }
        
        return result;
    }

    /**
     * 更新帖子状态
     */
    public int updateStatus(int id, int status) {
        int result = discussPostMapper.updateStatus(id, status);
        
        // 清除相关缓存
        if (result > 0) {
            cacheService.evictPostDetail(id);
            cacheService.evictPostList(0, 0);
            cacheService.evictPostList(0, 1);
        }
        
        return result;
    }

    /**
     * 更新帖子分数
     */
    public int updateScore(int postId, double score) {
        int result = discussPostMapper.updateScore(postId, score);
        
        // 清除相关缓存
        if (result > 0) {
            cacheService.evictPostDetail(postId);
            // 分数变化主要影响热门排序
            cacheService.evictPostList(0, 1);
        }
        
        return result;
    }

    /**
     * 预热缓存
     */
    public void warmUpCache() {
        logger.info("开始预热帖子缓存...");
        
        // 预热热门帖子
        try {
            List<DiscussPost> hotPosts = discussPostMapper.selectDiscussPosts(0, 0, 10, 1);
            logger.info("预热热门帖子缓存完成，共{}条", hotPosts.size());
        } catch (Exception e) {
            logger.error("预热热门帖子缓存失败", e);
        }
        
        // 预热最新帖子
        try {
            List<DiscussPost> latestPosts = discussPostMapper.selectDiscussPosts(0, 0, 10, 0);
            logger.info("预热最新帖子缓存完成，共{}条", latestPosts.size());
        } catch (Exception e) {
            logger.error("预热最新帖子缓存失败", e);
        }
        
        logger.info("帖子缓存预热完成");
    }
}
