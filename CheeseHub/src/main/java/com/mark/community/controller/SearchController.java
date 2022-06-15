package com.mark.community.controller;

import com.mark.community.entity.DiscussPost;
import com.mark.community.entity.Page;
import com.mark.community.service.CommentService;
import com.mark.community.service.ElasticsearchService;
import com.mark.community.service.LikeService;
import com.mark.community.service.UserService;
import com.mark.community.util.CommunityConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class SearchController {
    @Autowired
    private ElasticsearchService elasticsearchService;

    @Autowired
    private UserService userService;

    @Autowired
    private LikeService likeService;

    @Autowired
    private CommentService commentService;

    @RequestMapping(path="/search",method = RequestMethod.GET)
    public String search(String keyword, Page page , Model model){
        // 根据关键词搜索帖子
        org.springframework.data.domain.Page<DiscussPost> searchResult
                = elasticsearchService.searchDiscussPost(keyword, page.getCurrent() - 1, page.getLimit());
        // 聚合数据
        List<Map<String,Object>> discussPosts = new ArrayList<>();
        if(searchResult!=null){
            for (DiscussPost post : searchResult) {
                Map<String,Object> map = new HashMap<>();
                // 帖子
                map.put("post",post);
                // 作者
                map.put("user",userService.findUserById(post.getUserId()));
                // 点赞数量
                map.put("likeCount",likeService.findEntityLikeCount(CommunityConstant.ENTITY_TYPE_POST,post.getId()));

                discussPosts.add(map);
            }
        }
        model.addAttribute("discussPosts",discussPosts);
        model.addAttribute("keyword",keyword);

        // 设置分页信息
        page.setPath("/search?keyword="+keyword);
        page.setRows(searchResult==null?0: (int) searchResult.getTotalElements());
        return "/site/search";
    }
}
