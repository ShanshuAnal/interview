package com.nowcoder.community;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.annotation.PostConstruct;

/**
 * @author 19599
 */
@SpringBootApplication
@EnableScheduling
public class CommunityApplication {

	public static void main(String[] args) {
		SpringApplication.run(CommunityApplication.class, args);
	}

	/**
	 * 用来管理bean的生命周期，主要用来管理bean的初始化方法
	 * 这个注解修饰的方法会在构造器调用完之后被执行
	 */
	@PostConstruct
	public void init() {
		// 解决netty启动冲突问题
		System.setProperty("es.set.netty.runtime.available.processors", "false");
	}

}
