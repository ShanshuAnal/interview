package com.nowcoder.community.event;

import com.alibaba.fastjson.JSONObject;
import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Event;
import com.nowcoder.community.entity.Message;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.service.ElasticsearchService;
import com.nowcoder.community.service.MessageService;
import com.nowcoder.community.util.CommunityConstant;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * @author 19599
 */
@Component
public class EventConsumer implements CommunityConstant {

    private static final Logger logger = LoggerFactory.getLogger(EventConsumer.class);

    @Autowired
    private MessageService messageService;

    @Autowired
    private ElasticsearchService elasticsearchService;

    @Autowired
    private DiscussPostService discussPostService;

    @Value("${wk.image.storage}")
    private String wkImageStorage;

    @Value("${wk.image.command}")
    private String wkImageCommand;


    @Value("${aliyun.oss.accessKeyId}")
    private String accessKeyId;

    @Value("${aliyun.oss.accessKeySecret}")
    private String accessKeySecret;

    @Value("${aliyun.oss.endpoint}")
    private String endpoint;

    @Value("${aliyun.oss.buckets.shareBucket}")
    private String shareBucket;

    @Autowired
    private ThreadPoolTaskScheduler taskScheduler;

    /**
     * 消息通知事件
     *
     * @param record
     */
    @KafkaListener(topics = {TOPIC_COMMENT, TOPIC_LIKE, TOPIC_FOLLOW})
    public void handleCommentMessage(ConsumerRecord<String, Object> record) {
        if (!isValid(record)) {
            return;
        }
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);

        // 发送站内通知
        Message message = new Message();
        message.setFromId(SYSTEM_USER_ID);
        message.setToId(event.getEntityUserId());
        message.setConversationId(event.getTopic());
        message.setCreateTime(new Date());

        Map<String, Object> content = new HashMap<>();
        content.put("userId", event.getUserId());
        content.put("entityType", event.getEntityType());
        content.put("entityId", event.getEntityId());

        if (!event.getData().isEmpty()) {
            for (Map.Entry<String, Object> entry : event.getData().entrySet()) {
                content.put(entry.getKey(), entry.getValue());
            }
        }

        message.setContent(JSONObject.toJSONString(content));
        messageService.addMessage(message);
    }

    /**
     * 消费发帖事件
     *
     * @param record
     */
    @KafkaListener(topics = {TOPIC_PUBLISH})
    public void handlePublishMessage(ConsumerRecord<String, Object> record) {
        // 1. 安全性验证
        if (record == null || record.value() == null) {
            logger.error("消息的内容为空!");
            return;
        }
        // 从 Kafka 消息中解析出事件对象（Event）
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if (event == null) {
            logger.error("消息格式错误!");
            return;
        }
        // 查询帖子数据
        DiscussPost discussPost = discussPostService.findDiscussPostById(event.getEntityId());
        // 同步到 Elasticsearch
        elasticsearchService.saveDiscussPost(discussPost);
        logger.info("用户发布帖子: 用户ID={}, 标题={}", discussPost.getId(), discussPost.getTitle());
    }

    /**
     * 消费删帖事件
     *
     * @param record
     */
    @KafkaListener(topics = {TOPIC_DELETE})
    public void handleDeleteMessage(ConsumerRecord<String, Object> record) {
        // 验证消息是否合法
        if (!isValid(record)) {
            return;
        }
        // 将消息内容解析为 Event 对象
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        // 删除 Elasticsearch 中的帖子数据
        elasticsearchService.deleteDiscussPost(event.getEntityId());
        logger.info("用户删除帖子: 用户ID={}, 帖子ID={}", event.getUserId(), event.getEntityId());
    }

    /**
     * 处理分享事件
     *
     * @param record
     */
    @KafkaListener(topics = {TOPIC_SHARE})
    public void handleShareMessage(ConsumerRecord<String, Object> record) {
        if (!isValid(record)) {
            return;
        }
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        String htmlUrl = (String) event.getData().get("htmlUrl");
        String fileName = (String) event.getData().get("fileName");
        String suffix = (String) event.getData().get("suffix");

        // 拼接命令
        String cmd = wkImageCommand + " --quality 75 " + htmlUrl + " "
                + wkImageStorage + "/" + fileName + suffix;

        try {
            Runtime.getRuntime().exec(cmd);
            logger.info("生成长图成功：" + cmd);
        } catch (IOException e) {
            logger.error("生成长图失败：" + e.getMessage());
        }

        // 启用定时器，监视改图片是否生成，一旦生成了就上传到AliYunOss服务器
        UploadTask task = new UploadTask(fileName, suffix);
        // 0.5秒查询一次
        Future future = taskScheduler.scheduleAtFixedRate(task, 5000);
        task.setFuture(future);

    }


    class UploadTask implements Runnable {
        /**
         * 文件名
         */
        private String fileName;
        /**
         * 文件后缀
         */
        private String suffix;
        /**
         * 启动任务的返回值，可以用来停止定时器
         */
        private Future future;
        /**
         * 任务开始时间
         */
        private long startTime;
        /**
         * 上传次数
         */
        private int uploadCount;

        public void setFuture(Future future) {
            this.future = future;
        }

        public UploadTask(String fileName, String suffix) {
            this.fileName = fileName;
            this.suffix = suffix;
            this.startTime = System.currentTimeMillis();
        }

        @Override
        public void run() {
            // 生成图片失败
            if (System.currentTimeMillis() - startTime > 300000) {
                logger.error("执行时间过长，终止任务：" + fileName);
                future.cancel(true);
                return;
            }
            // 上传失败
            if (uploadCount >= 5) {
                logger.error("上传次数过多，终止任务：" + fileName);
                future.cancel(true);
                return;
            }

            uploadCount++;
            String path = wkImageStorage + "/" + fileName + suffix;
            File file = new File(path);

            if (file.exists()) {
                logger.info(String.format("开始第%d次上传[%s]", uploadCount, fileName));
                OSS ossClient = null;
                try {
                    ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
                    String filename = fileName + suffix;
                    // 上传文件到OSS
                    ossClient.putObject(shareBucket, filename, file);
                    // 获取文件访问URL
                    String fileOssUrl = "https://" + shareBucket + "." + endpoint + "/" + filename;
                    logger.info("文件上传成功，访问url：" + fileOssUrl);
                    future.cancel(true);
                } catch (OSSException e) {
                    logger.error("文件上传失败：" + e.getMessage());
                } catch (ClientException e) {
                    throw new RuntimeException(e);
                } finally {
                    if (ossClient != null) {
                        ossClient.shutdown();
                    }
                }
            } else {
                logger.info("等待图片生成[" + fileName + "].");
            }
        }
    }

    private boolean isValid(ConsumerRecord<String, Object> record) {
        if (record == null || record.value() == null) {
            logger.error("消息的内容为空!");
            return false;
        }
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if (event == null) {
            logger.error("消息格式错误!");
            return false;
        }
        return true;
    }

}
