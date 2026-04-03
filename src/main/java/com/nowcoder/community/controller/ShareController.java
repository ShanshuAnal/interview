package com.nowcoder.community.controller;

import com.nowcoder.community.entity.Event;
import com.nowcoder.community.event.EventProducer;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author: 19599
 * @Date: 2025/3/3 17:29
 * @Description:
 */
@Controller
public class ShareController implements CommunityConstant {

    private static final Logger logger = LoggerFactory.getLogger(ShareController.class);

    private final EventProducer eventProducer;

    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Value("${wk.image.storage}")
    private String wkImageStorage;

    @Value("${aliyun.oss.endpoint}")
    private String endpoint;

    @Value("${aliyun.oss.buckets.shareBucket}")
    private String shareBucket;

    public ShareController(EventProducer eventProducer) {
        this.eventProducer = eventProducer;
    }

    @GetMapping("/share")
    @ResponseBody
    public String share(String htmlUrl) {
        // 文件名
        String fileName = CommunityUtil.generateUUID();

        // 异步生成长图
        Event event = new Event()
                .setTopic(TOPIC_SHARE)
                .setData("htmlUrl", htmlUrl)
                .setData("fileName", fileName)
                .setData("suffix", ".png");
        eventProducer.fireEvent(event);

        // 返回访问路径
        Map<String, Object> map = new HashMap<>();
        map.put("shareUrl", "https://" + shareBucket + "." + endpoint + "/" + fileName + ".png");

        return CommunityUtil.getJSONString(0, null, map);
    }

    /**
     * 获取长图（废弃）
     * 改进：直接从aliyun oss服务器中获取share图片
     *
     * @param filename 文件名
     * @param response
     */
    //@GetMapping("/share/image/{filename}")
    public void getShareImage(@PathVariable String filename, HttpServletResponse response) {
        if (StringUtils.isBlank(filename)) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        response.setContentType("image/png");
        File file = new File(wkImageStorage + "/" + filename + ".png");
        FileInputStream fis = null;
        try {
            ServletOutputStream os = response.getOutputStream();
            fis = new FileInputStream(file);
            byte[] bytes = new byte[1024];
            int readCount = 0;
            while ((readCount = fis.read(bytes)) != -1) {
                os.write(bytes, 0, readCount);
            }
        } catch (IOException e) {
            logger.error("获取长图失败" + e.getMessage());
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    logger.error("输入流关闭失败：" + e.getMessage());
                }
            }
        }
    }
}






















