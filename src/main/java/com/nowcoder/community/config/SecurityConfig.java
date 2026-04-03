package com.nowcoder.community.config;

import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * @Author: 19599
 * @Date: 2025/3/1 17:20
 * @Description: 权限管理
 */
@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter implements CommunityConstant {
    // 1. 静态资源放行
    // 静态资源通常与业务逻辑无关，这些请求不需要权限控制，直接放行可以减少不必要的性能开销
    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring().antMatchers("/resources/**");
    }

    // 2. 设置权限空值规则
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // 2.1授权
        http.authorizeRequests()
                // （1）日常操作权限（用户，管理员，版主）
                .antMatchers(
                        "/users/setting",
                        "/user/upload",
                        "/discuss/add",
                        "/comment/add/**",
                        "letter/**",
                        "/notice/**",
                        "/like",
                        "/follow",
                        "/unfollow"
                )
                .hasAnyAuthority(
                        AUTHORITY_USER,
                        AUTHORITY_ADIMN,
                        AUTHORITY_MODERATOR
                )
                // （2）版主 特有权限
                .antMatchers(
                        // 置顶
                        "/discuss/top",
                        // 加精
                        "/discuss/wonderful"

                )
                .hasAnyAuthority(
                        AUTHORITY_MODERATOR
                )
                // （3）管理员 特有权限
                .antMatchers(
                        // 删帖
                        "/discuss/delete",
                        // 查看网站数据
                        "/data/**",
                        // 项目监控
                        "/actuator/**"
                )
                .hasAnyAuthority(
                        AUTHORITY_ADIMN
                )
                // （4）未明确列出的路径对所有用户开放
                .anyRequest().permitAll()
                // （5）禁用 CSRF 保护，适用于前后端分离的场景
                .and().csrf().disable();

        // 2.2异常处理（权限不够时的处理）
        http.exceptionHandling()
                // （1）没有登陆
                .authenticationEntryPoint(new AuthenticationEntryPoint() {
                    @Override
                    public void commence(HttpServletRequest request,
                                         HttpServletResponse response,
                                         AuthenticationException e)
                            throws IOException, ServletException {
                        handleUnauthorizedRequest(request, response, "Security:请登录后再操作");
                    }
                })
                // （2）权限不足
                .accessDeniedHandler(new AccessDeniedHandler() {
                    @Override
                    public void handle(HttpServletRequest request,
                                       HttpServletResponse response,
                                       AccessDeniedException e)
                            throws IOException, ServletException {
                        handleUnauthorizedRequest(request, response, "Security:没有访问权限");
                    }
                });

        // 2.3自定义登出逻辑
        // 覆盖 Spring Security 默认的登出逻辑，使用自定义的登出路径
        http.logout().logoutUrl("/securitylogout");
    }

    // 提取公共方法，处理未登录或权限不足的请求
    private void handleUnauthorizedRequest(HttpServletRequest request,
                                           HttpServletResponse response,
                                           String message) throws IOException {
        // 获取请求头
        String header = request.getHeader("x-requested-with");
        // 如果是Ajax请求，返回 JSON 格式的错误信息
        if ("XMLHttpRequest".equals(header)) {
            response.setContentType("application/plain;charset=utf-8");
            PrintWriter writer = response.getWriter();
            writer.write(CommunityUtil.getJSONString(403, message));
        } else {
            // 否则重定向到权限不足页面
            response.sendRedirect(request.getContextPath() + "/denied");
        }
    }
}