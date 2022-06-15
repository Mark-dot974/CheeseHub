package com.mark.community;

import com.mark.community.entity.DiscussPost;
import com.mark.community.service.DiscussPostService;
import com.mark.community.service.LikeService;
import com.mark.community.util.CommunityConstant;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class Testscore {
    @Autowired
    DiscussPostService discussPostService;
    @Autowired
    LikeService likeService;

    private static final Date epoch;
    static {
        try {
            epoch  = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2022-5-20 00:00:00");
        } catch (ParseException e) {
            throw new RuntimeException("初始化项目纪元失败",e);
        }
    }
    @Test
    public void TestScore(){
        DiscussPost discussPost = discussPostService.findDiscussPostById(288);
        // 是否精华
        boolean wonderful = discussPost.getStatus() == 1;
        // 评论数量
        int commentCount = discussPost.getCommentCount();
        // 点赞数量
        long likeCount = likeService.findEntityLikeCount(CommunityConstant.ENTITY_TYPE_POST,discussPost.getId());

        // 公式：log（精华分+评论数*10+点赞数*2）+（发布时间-项目纪元）

        // 计算权重
        double w = (wonderful ? 75 : 0) + commentCount * 10 + likeCount * 2;
        // 分数 = 帖子权重 （w可能为0，所以和1取大） + 距离天数
        double score = Math.log10(Math.max(w,1))
                + (discussPost.getCreateTime().getTime() - epoch.getTime())/(1000*3600*24);
        System.out.println(score);
        score = (discussPost.getCreateTime().getTime() - epoch.getTime())/(1000*3600*24);
        System.out.println("timescore:"+score);
        System.out.println("wight:"+w);
    }
}
