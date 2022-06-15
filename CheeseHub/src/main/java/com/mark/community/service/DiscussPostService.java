package com.mark.community.service;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.mark.community.dao.DiscussPostMapper;
import com.mark.community.entity.DiscussPost;
import com.mark.community.util.CommunityUtil;
import com.mark.community.util.SensitiveFilter;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class DiscussPostService {
    private static final Logger logger = LoggerFactory.getLogger(DiscussPostService.class);
    @Autowired
    private DiscussPostMapper discussPostMapper;
    @Autowired
    private SensitiveFilter sensitiveFilter;
    @Value("${caffeine.posts.max-size}")
    private int maxSize;
    @Value("${caffeine.posts.expire-seconds}")
    private int expireSeconds;

    // 帖子列表缓存
    private LoadingCache<String,List<DiscussPost>> postListCache;
    // 帖子总数缓存,因为每次分页都要显示总页数，而总页数并不频繁更新。
    private LoadingCache<Integer,Integer> postRowsCache;

    /**
     * init()方法创建 postListCache 和 postRowsCache 这两个容器，并且设置了这两个容器的一些规则。但是并未从中装东西
     * 只有当访问具体操作时，才会调用容器的load方法，从数据库中查询，初始化该容器。
     */
    @PostConstruct
    public void init(){
        // 初始化帖子列表缓存(从数据库查询)
        postListCache=Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireSeconds, TimeUnit.SECONDS)
                .build(new CacheLoader<String, List<DiscussPost>>() {
                    @Override
                    public @Nullable List<DiscussPost> load(@NonNull String key) throws Exception {
                        if(key == null || key.length() == 0){
                            throw new IllegalArgumentException("参数错误！");
                        }
                        String [] params = key.split(":");
                        if(params == null || params.length!=2){
                            throw new IllegalArgumentException("参数错误!");
                        }
                        int offset = Integer.valueOf(params[0]);
                        int limit = Integer.valueOf(params[1]);
                        logger.debug("load post from DB");
                        return discussPostMapper.selectDiscussPosts(0,offset,limit,1);
                    }
                });
        // 初始化帖子总数缓存(从数据库查询)
        postRowsCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireSeconds,TimeUnit.SECONDS)
                .build(new CacheLoader<Integer, Integer>() {
                    @Override
                    public @Nullable Integer load(@NonNull Integer key) throws Exception {
                        logger.debug("load post rows from DB");
                        return discussPostMapper.selectDiscussPostRows(key);
                    }
                });
    }

    /**
     * 查找userId的帖子，如果不传userId就是查询所有用户的帖子。
     * 查询时，先从缓存中查找，缓存中没有才从数据库中查
     * @param userId
     * @param offset
     * @param limit
     * @param orderMode
     * @return
     */
    public List<DiscussPost> findDiscussPosts(int userId,int offset,int limit,int orderMode){
        // 只有是查询热帖时才从缓存中查询
        if(userId == 0 && orderMode == 1){
            return postListCache.get(offset+":"+limit);
        }
        return discussPostMapper.selectDiscussPosts(userId,offset,limit,orderMode);
    }
    public int findDiscussPostRows(int userId){
        if(userId == 0){
            return postRowsCache.get(userId);
        }
        return discussPostMapper.selectDiscussPostRows(userId);
    }

    public int addDiscussPost(DiscussPost post){
        if(post == null){
            throw new IllegalArgumentException("参数不能为空");
        }
        // 转义HTML标记，防止输入内容被显示为网页标记语言。
        // eg：<srcipt>Test Publish</script>  ====》 &lt;script&gt;Test Publish&lt;/script&gt;
        post.setTitle(HtmlUtils.htmlEscape(post.getTitle()));
        post.setContent(HtmlUtils.htmlEscape(post.getContent()));
        // 过滤敏感词
        post.setTitle(sensitiveFilter.filter(post.getTitle()));
        post.setContent(sensitiveFilter.filter(post.getContent()));

        return discussPostMapper.insertDiscussPost(post);
    }

    public DiscussPost showDetail(int id){
        return discussPostMapper.selectDiscussPostById(id);
    }

    public void updateCommentCount(int entityId, int count) {
        discussPostMapper.updateCommentCount(entityId,count);
    }

    public DiscussPost findDiscussPostById(int entityId) {
        return discussPostMapper.selectDiscussPostById(entityId);
    }

    // 获取帖子的类型（置顶 （1）or 普通 （0））
    public int getDiscussPostType(int id){
        return discussPostMapper.selectDiscussPostType(id);
    }

    // 更改某个帖子的类型
    public int updateDiscussPostType(int discussId,int type){
        return discussPostMapper.updateDiscussType(discussId,type);
    }

    // 获取帖子的状态（加精1，拉黑（删除）2，普通0）
    public int getDiscussPostStatus(int id){
        return discussPostMapper.selectDiscussPostStatus(id);
    }

    // 更改某个帖子的状态
    public int updateDiscussPostStatus(int id,int status){
        return discussPostMapper.updateDiscussStatus(id,status);
    }

    // 更改帖子的分数
    public int updateScore(int id,double score){
        return discussPostMapper.updateScore(id,score);
    }
}
