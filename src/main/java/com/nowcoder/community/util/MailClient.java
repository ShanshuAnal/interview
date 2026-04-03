package com.nowcoder.community.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

/**
 * @author 19599
 */
@Component
public class MailClient {

    private static final Logger logger = LoggerFactory.getLogger(MailClient.class);

    /**
     * Spring 提供的邮件发送接口，封装了底层的邮件发送逻辑。
     * 通常在配置文件中通过 spring.mail.* 属性进行配置
     */
    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;

    /**
     * 发送邮件
     *
     * @param to      收件人的邮箱地址
     * @param subject 标题
     * @param content 正文
     */
    public void sendMail(String to, String subject, String content) {
        try {
            // 生成一个完整的邮件消息
            MimeMessage message = mailSender.createMimeMessage();
            // Spring 提供的辅助类，用于简化 MimeMessage 的操作
            // 支持设置发件人、收件人、主题、内容等功能
            MimeMessageHelper helper = new MimeMessageHelper(message);
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            // 第二个参数为 true，表示支持HTML格式。如果为 false，则内容会被视为纯文本
            helper.setText(content, true);
            // 发送
            mailSender.send(helper.getMimeMessage());
        } catch (MessagingException e) {
            logger.error("发送邮件失败:" + e.getMessage());
        }
    }
}
