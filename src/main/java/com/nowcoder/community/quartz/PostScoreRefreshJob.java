package com.nowcoder.community.quartz;

import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.service.ElasticsearchService;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.RedisKeyUtil;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @Author: 19599
 * @Date: 2025/3/3 2:38
 * @Description: 帖子得分刷新
 */
@Component
public class PostScoreRefreshJob implements Job, CommunityConstant {

    public static final Logger logger = LoggerFactory.getLogger(PostScoreRefreshJob.class);

    private final RedisTemplate<String, Object> redisTemplate;

    private final DiscussPostService discussPostService;

    private final LikeService likeService;

    private final ElasticsearchService elasticsearchService;

    /**
     * 牛客元年
     */
    private static final Date epoch;

    /**
     * 初始化牛客元年
     */
    static {
        try {
            epoch = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2014-08-01 00:00:00");
        } catch (ParseException e) {
            throw new RuntimeException("初始化元年事件失败：" + e.getMessage());
        }
    }

    public PostScoreRefreshJob(RedisTemplate<String, Object> redisTemplate,
                               DiscussPostService discussPostService,
                               LikeService likeService,
                               ElasticsearchService elasticsearchService) {
        this.redisTemplate = redisTemplate;
        this.discussPostService = discussPostService;
        this.likeService = likeService;
        this.elasticsearchService = elasticsearchService;
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        String redisKey = RedisKeyUtil.getPostScoreKey();
        // <"post:score", postId>
        BoundSetOperations<String, Object> operations = redisTemplate.boundSetOps(redisKey);
        if (operations.size() == 0) {
            logger.info("[任务取消] 没有需要刷新的帖子");
            return;
        }
        logger.info("[任务开始] 正在刷新帖子分数：" + operations.size());
        while (operations.size() > 0) {
            // 此处弹出的是帖子id，强转成int类型
            // 刷新该帖子分数
            refresh((Integer) operations.pop());
        }
        logger.info("[任务结束] 帖子分数刷新完毕！");
    }

    /**
     * 刷新一个帖子的分数
     * 分数= log(精华分 + 评论数 * 10 + 点赞数 * 2) + (发布时间 - 牛客元年)
     *
     * @param postId 帖子id
     */
    private void refresh(int postId) {
        DiscussPost discussPost = discussPostService.findDiscussPostById(postId);
        if (discussPost == null) {
            logger.error("该帖子不存在：id = " + postId);
            return;
        }
        // 是否精华
        boolean wonderful = discussPost.getStatus() == 1;
        // 评论数
        int commentCount = discussPost.getCommentCount();
        // 点赞数
        long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST, postId);
        // 计算权重
        double w = (wonderful ? 75 : 0) + commentCount * 10L + likeCount * 2;
        // 分数 = 帖子权重 + 距离天数
        double score = Math.log10(Math.max(w, 1)) + (double) (discussPost.getCreateTime().getTime() - epoch.getTime()) / (1000 * 3600 * 24);
        // 更新帖子分数
        discussPostService.updateScore(postId, score);
        // 同步搜索elastic服务器
        discussPost.setScore(score);
        elasticsearchService.saveDiscussPost(discussPost);
        logger.info("[刷新帖子分数] 帖子ID: {}, 分数: {}", postId, score);
    }
}
