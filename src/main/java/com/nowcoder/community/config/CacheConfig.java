package com.nowcoder.community.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 缓存配置类
 * 统一管理缓存相关参数
 */
@Component
@ConfigurationProperties(prefix = "cache")
public class CacheConfig {

    private Post post = new Post();
    private Local local = new Local();

    public Post getPost() {
        return post;
    }

    public void setPost(Post post) {
        this.post = post;
    }

    public Local getLocal() {
        return local;
    }

    public void setLocal(Local local) {
        this.local = local;
    }

    public static class Post {
        private int detailExpireSeconds = 7200;
        private int listExpireSeconds = 1800;
        private int countExpireSeconds = 3600;

        public int getDetailExpireSeconds() {
            return detailExpireSeconds;
        }

        public void setDetailExpireSeconds(int detailExpireSeconds) {
            this.detailExpireSeconds = detailExpireSeconds;
        }

        public int getListExpireSeconds() {
            return listExpireSeconds;
        }

        public void setListExpireSeconds(int listExpireSeconds) {
            this.listExpireSeconds = listExpireSeconds;
        }

        public int getCountExpireSeconds() {
            return countExpireSeconds;
        }

        public void setCountExpireSeconds(int countExpireSeconds) {
            this.countExpireSeconds = countExpireSeconds;
        }
    }

    public static class Local {
        private Post post = new Post();

        public Post getPost() {
            return post;
        }

        public void setPost(Post post) {
            this.post = post;
        }

        public static class Post {
            private int detailMaxSize = 1000;
            private int listMaxSize = 500;
            private int countMaxSize = 100;

            public int getDetailMaxSize() {
                return detailMaxSize;
            }

            public void setDetailMaxSize(int detailMaxSize) {
                this.detailMaxSize = detailMaxSize;
            }

            public int getListMaxSize() {
                return listMaxSize;
            }

            public void setListMaxSize(int listMaxSize) {
                this.listMaxSize = listMaxSize;
            }

            public int getCountMaxSize() {
                return countMaxSize;
            }

            public void setCountMaxSize(int countMaxSize) {
                this.countMaxSize = countMaxSize;
            }
        }
    }
} 