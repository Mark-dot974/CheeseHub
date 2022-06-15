package com.mark.community.dao;

import com.mark.community.entity.DiscussPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
@Mapper
public interface DiscussPostMapper {
    /**
     * 查询userid对应用户的从offset开始的limit条帖子。若是userID为0，则查询所有用户的帖子。
     *
     * @param id
     * @param userId     0为所有用户，不为0为指定用户
     * @param offset    起始位置
     * @param limit     查几条
     * @param orderMode
     * @return
     */
    List<DiscussPost> selectDiscussPosts(int userId,int offset, int limit, int orderMode);

    /**
     * 查询userId的用户共有几条帖子
     * @param userId    0为所有用户，不为0为指定用户
     * @return
     */
    // @Param注解用于给参数取别名
    // 如果只有一个参数，并且在<if>里使用，则必须加别名。
    int selectDiscussPostRows(@Param("userId") int userId);

    int insertDiscussPost(DiscussPost discussPost);

    DiscussPost selectDiscussPostById(int id);

    void updateCommentCount(int id, int commentCount);

    int updateDiscussType(int discussId, int type);

    int updateDiscussStatus(int id,int status);

    int selectDiscussPostType(int id);

    int selectDiscussPostStatus(int id);

    int updateScore(int id, double score);
}
