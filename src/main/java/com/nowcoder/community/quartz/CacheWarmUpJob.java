package com.nowcoder.community.quartz;

import com.nowcoder.community.service.DiscussPostService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 缓存预热任务
 * 定时预热帖子缓存，提高系统性能
 */
@Component
public class CacheWarmUpJob implements Job {

    private static final Logger logger = LoggerFactory.getLogger(CacheWarmUpJob.class);

    @Autowired
    private DiscussPostService discussPostService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        logger.info("开始执行缓存预热任务...");
        
        try {
            discussPostService.warmUpCache();
            logger.info("缓存预热任务执行完成");
        } catch (Exception e) {
            logger.error("缓存预热任务执行失败", e);
            throw new JobExecutionException(e);
        }
    }
} 