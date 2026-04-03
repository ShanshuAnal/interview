package com.nowcoder.community.actuator;

import com.nowcoder.community.util.CommunityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * @Author: 19599
 * @Date: 2025/3/4 1:33
 * @Description: 数据库连接池监控
 */
@Endpoint(id = "database")
@Component
public class DatabaseEndPoint {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseEndPoint.class);

    private final DataSource dataSource;

    public DatabaseEndPoint(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @ReadOperation
    public String checkConnention() {
        try (Connection conn = dataSource.getConnection()) {
            return CommunityUtil.getJSONString(0, "获取数据库连接成功");
        } catch (SQLException e) {
            logger.error("获取连接失败：" + e.getMessage());
            return CommunityUtil.getJSONString(1, "获取数据库连接失败");
        }
    }
}
