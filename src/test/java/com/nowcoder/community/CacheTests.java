package com.nowcoder.community;

import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.service.DiscussPostCacheService;
import com.nowcoder.community.service.DiscussPostService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

/**
 * 缓存功能测试
 */
@SpringBootTest
public class CacheTests {

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private DiscussPostCacheService cacheService;

    @Test
    public void testPostDetailCache() {
        System.out.println("=== 测试帖子详情缓存 ===");
        
        // 第一次查询，应该从数据库加载
        long start1 = System.currentTimeMillis();
        DiscussPost post1 = discussPostService.findDiscussPostById(1);
        long time1 = System.currentTimeMillis() - start1;
        System.out.println("第一次查询耗时: " + time1 + "ms");
        
        // 第二次查询，应该从缓存加载
        long start2 = System.currentTimeMillis();
        DiscussPost post2 = discussPostService.findDiscussPostById(1);
        long time2 = System.currentTimeMillis() - start2;
        System.out.println("第二次查询耗时: " + time2 + "ms");
        
        System.out.println("缓存命中率提升: " + (time1 - time2) + "ms");
    }

    @Test
    public void testPostListCache() {
        System.out.println("=== 测试帖子列表缓存 ===");
        
        // 第一次查询，应该从数据库加载
        long start1 = System.currentTimeMillis();
        List<DiscussPost> posts1 = discussPostService.findDiscussPosts(0, 0, 10, 1);
        long time1 = System.currentTimeMillis() - start1;
        System.out.println("第一次查询耗时: " + time1 + "ms");
        System.out.println("查询到帖子数量: " + posts1.size());
        
        // 第二次查询，应该从缓存加载
        long start2 = System.currentTimeMillis();
        List<DiscussPost> posts2 = discussPostService.findDiscussPosts(0, 0, 10, 1);
        long time2 = System.currentTimeMillis() - start2;
        System.out.println("第二次查询耗时: " + time2 + "ms");
        System.out.println("查询到帖子数量: " + posts2.size());
        
        System.out.println("缓存命中率提升: " + (time1 - time2) + "ms");
    }

    @Test
    public void testPostCountCache() {
        System.out.println("=== 测试帖子总数缓存 ===");
        
        // 第一次查询，应该从数据库加载
        long start1 = System.currentTimeMillis();
        int count1 = discussPostService.findDiscussPostRows(0);
        long time1 = System.currentTimeMillis() - start1;
        System.out.println("第一次查询耗时: " + time1 + "ms");
        System.out.println("帖子总数: " + count1);
        
        // 第二次查询，应该从缓存加载
        long start2 = System.currentTimeMillis();
        int count2 = discussPostService.findDiscussPostRows(0);
        long time2 = System.currentTimeMillis() - start2;
        System.out.println("第二次查询耗时: " + time2 + "ms");
        System.out.println("帖子总数: " + count2);
        
        System.out.println("缓存命中率提升: " + (time1 - time2) + "ms");
    }

    @Test
    public void testCacheEviction() {
        System.out.println("=== 测试缓存清除 ===");
        
        // 先查询一次，确保缓存中有数据
        DiscussPost post = discussPostService.findDiscussPostById(1);
        System.out.println("缓存帖子: " + post.getTitle());
        
        // 清除缓存
        cacheService.evictPostDetail(1);
        System.out.println("已清除帖子详情缓存");
        
        // 再次查询，应该从数据库重新加载
        long start = System.currentTimeMillis();
        DiscussPost post2 = discussPostService.findDiscussPostById(1);
        long time = System.currentTimeMillis() - start;
        System.out.println("重新查询耗时: " + time + "ms");
        System.out.println("重新查询帖子: " + post2.getTitle());
    }

    @Test
    public void testCacheWarmUp() {
        System.out.println("=== 测试缓存预热 ===");
        
        long start = System.currentTimeMillis();
        discussPostService.warmUpCache();
        long time = System.currentTimeMillis() - start;
        
        System.out.println("缓存预热完成，耗时: " + time + "ms");
    }
} 