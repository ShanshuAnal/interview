package com.nowcoder.community.entity;

import java.util.Date;

/**
 * @author 19599
 * 消息类：
 * 分为两大类：系统通知、用户私信
 * 系统通知需要借助Kafka消息队列，相当于系统向目标用户发的私信，会话id是通知的主题（评论、点赞、关注）
 * 用户私信就直接将私信消息对象插入数据库中就行了
 */
public class Message {
    private int id;
    /**
     * 发送用户id
     */
    private int fromId;
    /**
     * 接受用户id
     */
    private int toId;
    /**
     * 会话id
     */
    private String conversationId;
    /**
     * 聊天内容
     */
    private String content;
    /**
     * 消息状态
     * 0 未读
     * 1 已读
     * 2 删除
     */
    private int status;
    /**
     * 创建时间
     */
    private Date createTime;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getFromId() {
        return fromId;
    }

    public void setFromId(int fromId) {
        this.fromId = fromId;
    }

    public int getToId() {
        return toId;
    }

    public void setToId(int toId) {
        this.toId = toId;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    @Override
    public String toString() {
        return "Message{" +
                "id=" + id +
                ", fromId=" + fromId +
                ", toId=" + toId +
                ", conversationId='" + conversationId + '\'' +
                ", content='" + content + '\'' +
                ", status=" + status +
                ", createTime=" + createTime +
                '}';
    }
}
