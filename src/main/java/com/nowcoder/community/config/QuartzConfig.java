package com.nowcoder.community.config;

import com.nowcoder.community.quartz.PostScoreRefreshJob;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;

/**66
 * @Author: 19599
 * @Date: 2025/3/3 0:17
 * @Description: 配置 -> 数据库 -> 调用
 * 任务调度功能，并实现了定时计算帖⼦分数、定时清理垃圾⽂件
 */
@Configuration
public class QuartzConfig {
    /*
     * BeanFactory 是Spring IoC容器的顶层接口
     * FactoryBean 可简化Bean的实例化过程
     * 1. 通过FactoryBean封装了Bean的实例化过程
     * 2. 将FactoryBean装配到Spring IoC容器中
     * 3. 将FactoryBean注入给其他的Bean
     * 4. 其他的Bean得到的是FactoryBean所管理的对象实例
     */



    /**
     * 刷新帖子分数任务
     *
     * @return
     */
    @Bean
    public JobDetailFactoryBean postScoreRefreshJobDetail() {
        JobDetailFactoryBean factoryBean = new JobDetailFactoryBean();
        factoryBean.setJobClass(PostScoreRefreshJob.class);
        factoryBean.setName("postScoreRefreshJob");
        factoryBean.setGroup("communityGroup");
        factoryBean.setDurability(true);
        factoryBean.setRequestsRecovery(true);

        return factoryBean;
    }

    /**
     * 刷新帖子分数任务的触发器
     *
     * @param postScoreRefreshJobDetail
     * @return
     */
    @Bean
    public SimpleTriggerFactoryBean postScoreRefreshTrigger(JobDetail postScoreRefreshJobDetail) {
        SimpleTriggerFactoryBean factoryBean = new SimpleTriggerFactoryBean();
        factoryBean.setJobDetail(postScoreRefreshJobDetail);
        factoryBean.setName("postScoreRefreshTrigger");
        factoryBean.setGroup("communityTriggerGroup");
        factoryBean.setRepeatInterval(1000 * 60 * 5);
        factoryBean.setJobDataMap(new JobDataMap());

        return factoryBean;
    }

}
















