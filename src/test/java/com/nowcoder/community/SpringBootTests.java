package com.nowcoder.community;

import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.service.DiscussPostService;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;

/**
 * @Author: 19599
 * @Date: 2025/3/4 0:42
 * @Description:
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class SpringBootTests {

    @Autowired
    private DiscussPostService postService;

    private DiscussPost post;
    @Autowired
    private DiscussPostService discussPostService;


    @BeforeClass
    public static void beforeClass() {
        System.out.println("before class");
    }

    @AfterClass
    public static void afterClass() {
        System.out.println("after class");
    }

    @Before
    public void before() {
        System.out.println("before method");

        // 初始化测试数据
        post = new DiscussPost();
        post.setUserId(111);
        post.setTitle("test title");
        post.setContent("test content");
        post.setCreateTime(new Date());
        postService.addDiscussPost(post);
    }

    @After
    public void after() {
        System.out.println("after method");

        // 删除测试数据
        postService.updateStatus(post.getUserId(), 2);
    }

    @Test
    public void test1() {
        System.out.println("tests1");

    }

    @Test
    public void test2() {
        System.out.println("tests2");
    }

    @Test
    public void testFindById() {
        DiscussPost discussPost = postService.findDiscussPostById(post.getId());
        Assert.assertNotNull(discussPost);
        Assert.assertEquals(post.getTitle(), discussPost.getTitle());
        Assert.assertEquals(post.getContent(), discussPost.getContent());

    }

    @Test
    public void testUpdate() {
        int rows = discussPostService.updateScore(post.getId(), 1000.00);
        Assert.assertEquals(1, rows);

        DiscussPost discussPost = postService.findDiscussPostById(post.getId());
        Assert.assertNotNull(discussPost);
        Assert.assertEquals(1000.00, discussPost.getScore(), 2);
    }
}
