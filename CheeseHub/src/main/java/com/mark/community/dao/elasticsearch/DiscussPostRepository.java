package com.mark.community.dao.elasticsearch;

import com.mark.community.entity.DiscussPost;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
// 泛型：<要处理的类型，主键的类型>
public interface DiscussPostRepository extends ElasticsearchRepository<DiscussPost,Integer> {
}
