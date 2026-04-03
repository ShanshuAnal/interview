package com.nowcoder.community.service;

import com.nowcoder.community.dao.elasticsearch.DiscussPostRepository;
import com.nowcoder.community.entity.DiscussPost;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.SearchResultMapper;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.aggregation.impl.AggregatedPageImpl;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @Author: 19599
 * @Date: 2025/3/1 21:22
 * @Description:
 */
@Service
public class ElasticsearchService {

    private final DiscussPostRepository discussPostRepository;

    private final ElasticsearchTemplate elasticsearchTemplate;

    public ElasticsearchService(DiscussPostRepository discussPostRepository, ElasticsearchTemplate elasticsearchTemplate) {
        this.discussPostRepository = discussPostRepository;
        this.elasticsearchTemplate = elasticsearchTemplate;
    }

    /**
     * 往elastic服务器中添加帖子
     *
     * @param discussPost 帖子
     */
    @Retryable(value = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public void saveDiscussPost(DiscussPost discussPost) {
        discussPostRepository.save(discussPost);
    }

    /**
     * 删除elastic服务器中的帖子
     *
     * @param discussId 帖子id
     */
    @Retryable(value = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public void deleteDiscussPost(int discussId) {
        discussPostRepository.deleteById(discussId);
    }

    /**
     * 搜索elasticsearch服务器中的帖子
     *
     * @param keyword 关键词
     * @param current 起始页
     * @param limit   最大行数
     * @return
     */
    public Page<DiscussPost> searchDiscussPost(String keyword, int current, int limit) {
        SearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders
                        .multiMatchQuery(keyword, "title", "content"))
                .withSort(SortBuilders.fieldSort("type").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("status").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC))
                .withPageable(PageRequest.of(current, limit))
                .withHighlightFields(
                        new HighlightBuilder.Field("title").preTags("<em>").postTags("</em>"),
                        new HighlightBuilder.Field("content").preTags("<em>").postTags("</em>")
                ).build();

        return elasticsearchTemplate.queryForPage(searchQuery, DiscussPost.class, new SearchResultMapper() {
            @Override
            public <T> AggregatedPage<T> mapResults(SearchResponse response, Class<T> aClass, Pageable pageable) {
                SearchHits hits = response.getHits();
                if (hits.getTotalHits() <= 0) {
                    return null;
                }
                List<DiscussPost> list = new ArrayList<>();
                for (SearchHit hit : hits) {
                    // 构造DiscussPost对象
                    DiscussPost discussPost = new DiscussPost();
                    String id = hit.getSourceAsMap().get("id").toString();
                    discussPost.setId(Integer.parseInt(id));
                    discussPost.setUserId(Integer.parseInt(hit.getSourceAsMap().get("userId").toString()));
                    discussPost.setContent(hit.getSourceAsMap().get("content").toString());
                    discussPost.setTitle(hit.getSourceAsMap().get("title").toString());
                    discussPost.setStatus(Integer.parseInt(hit.getSourceAsMap().get("status").toString()));
                    discussPost.setCreateTime(new Date(Long.parseLong(hit.getSourceAsMap().get("createTime").toString())));
                    discussPost.setCommentCount(Integer.parseInt(hit.getSourceAsMap().get("commentCount").toString()));

                    // 处理高亮显示的结果
                    HighlightField titleField = hit.getHighlightFields().get("title");
                    if (titleField != null) {
                        discussPost.setTitle(titleField.getFragments()[0].toString());
                    }
                    HighlightField contentField = hit.getHighlightFields().get("content");
                    if (contentField != null) {
                        discussPost.setContent(contentField.getFragments()[0].toString());
                    }

                    list.add(discussPost);
                }
                return new AggregatedPageImpl(
                        list,
                        pageable,
                        hits.getTotalHits(),
                        response.getAggregations(),
                        response.getScrollId(),
                        hits.getMaxScore());
            }
        });
    }
}
