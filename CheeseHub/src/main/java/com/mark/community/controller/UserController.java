package com.mark.community.controller;

import com.mark.community.annotation.LoginRequired;
import com.mark.community.entity.DiscussPost;
import com.mark.community.entity.Page;
import com.mark.community.entity.User;
import com.mark.community.service.*;
import com.mark.community.util.CommunityConstant;
import com.mark.community.util.CommunityUtil;
import com.mark.community.util.HostHolder;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringExclude;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/user")
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Value("${community.path.upload}")
    private String uploadPath;

    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    private UserService userService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private LikeService likeService;

    @Autowired
    private FollowService followService;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private CommentService commentService;

    @LoginRequired
    @RequestMapping("/setting")
    public String getSettingPage(){
        return "/site/setting";
    }

    @LoginRequired
    @RequestMapping(path="/upload",method= RequestMethod.POST)
    public String uploadHeader(MultipartFile headerImage, Model model, HttpServletRequest request){
        if(headerImage == null ){
            model.addAttribute("erro","您还没有选择图片!");
            return "/site/setting";
        }
        // 生成随机文件名
        String fileName = headerImage.getOriginalFilename();
        String suffix = fileName.substring(fileName.lastIndexOf("."));
        if(StringUtils.isBlank(suffix)){
            model.addAttribute("error","文件的格式不正确");
            return "/site/setting";
        }
        fileName = CommunityUtil.generateUUID()+suffix;
        // 确定文件存放的路径
        File directory = new File(uploadPath);
        if(!directory.exists()) {
            directory.mkdirs();
        }
        File dest = new File(directory,File.separator+fileName);
        try {
            headerImage.transferTo(dest);
        } catch (IOException e) {
            logger.error("上传文件失败: " + e.getMessage());
            throw new RuntimeException("上传文件失败,服务器发生异常!", e);
        }

        // 更新当前用户的头像路径（不应该向用户暴露图片的存放路径，而应该存放请求图片的路径,图片src是一个controller请求）
        // http://localhost:8080/community/user/header/xxx.png
        User user = hostHolder.getUser();
        String headUrl = domain + contextPath + "/user/header/" + fileName;
        userService.updateHeader(user.getId(),headUrl);
        return "redirect:/index";
    }

    @RequestMapping(path="/header/{fileName}",method = RequestMethod.GET)
    public void getHeader(@PathVariable("fileName") String fileName , HttpServletResponse response,HttpServletRequest request)
    {
        // 服务器存放路径
        fileName = uploadPath + File.separator + fileName;
        // 文件后缀
        String suffix = fileName.substring(fileName.lastIndexOf("."));
        // 响应图片
        response.setContentType("image/"+suffix);
        try(
                FileInputStream fis = new FileInputStream(fileName);
                OutputStream os = response.getOutputStream();
                ){
            byte [] buffer = new byte[1024];
            int b = 0;
            while((b=fis.read(buffer))!=-1){
                os.write(buffer,0,b);
            }
        }catch (Exception e){
            logger.error("读取头像失败: " + e.getMessage());
        }
    }

    @LoginRequired
    @RequestMapping(path="/editPassword",method= RequestMethod.POST)
    public String editPassword(String originalPassword,String newPassword,String confirmPassword,Model model){
        // 验证用户输入的密码是否正确
        User user = hostHolder.getUser();
        String encodedPassword = CommunityUtil.md5(originalPassword+user.getSalt());
        if(!user.getPassword().equals(encodedPassword)){
            model.addAttribute("passwordErro","输入的密码不正确");
            return "/site/setting";
        }
        if(!newPassword.equals(confirmPassword)){
            model.addAttribute("passwordDifferentErro","两次输入的密码不一致！");
            return "/site/setting";
        }
        newPassword = CommunityUtil.md5(newPassword+user.getSalt());
        userService.editPassword(user.getId(),newPassword);
        return "redirect:/index";
    }

    @RequestMapping(path = "/profile/{userId}",method = RequestMethod.GET)
    public String getProfilePage(@PathVariable("userId") int userId , Model model){
        User user = userService.findUserById(userId);
        if(user == null){
                throw new RuntimeException("用户不存在！");
        }
        model.addAttribute("user",user);
        // 查询用户获得的点赞数量
        int likeCount = likeService.findUserLikeCount(user.getId());
        model.addAttribute("likeCount",likeCount);

        // 查询用户关注 以及 被关注人数
        long followeeCount = followService.findFolloweeCount(userId, CommunityConstant.ENTITY_TYPE_USER);
        model.addAttribute("followeeCount",followeeCount);
        long followerCount = followService.findFollowerCount(CommunityConstant.ENTITY_TYPE_USER, userId);
        model.addAttribute("followerCount",followerCount);

        boolean hasFollowed = false;
        if(user != null)
        {
            if(hostHolder.getUser()!=null)
                hasFollowed = followService.hasFollowed(hostHolder.getUser().getId(),CommunityConstant.ENTITY_TYPE_USER,userId);
        }
        model.addAttribute("hasFollowed", hasFollowed);

        return "/site/profile";
    }

    @RequestMapping(path = "/my-post",method = RequestMethod.GET)
    public String getMyPostsPage(Model model, Page page){
        User user = hostHolder.getUser();
        // 设置分页信息
        int totalRows = discussPostService.findDiscussPostRows(user.getId());
        page.setRows(totalRows);
        page.setPath("/my-post");

        List<DiscussPost> discussPosts = discussPostService.findDiscussPosts(user.getId(), page.getOffset(), page.getLimit(), 0);
        List<Map<String,Object>> myPostsVo = new ArrayList<>();
        if(discussPosts != null ){
            for (DiscussPost discussPost : discussPosts) {
                Map<String,Object> myPost = new HashMap<>();
                myPost.put("post",discussPost);
                long likeCount = likeService.findEntityLikeCount(CommunityConstant.ENTITY_TYPE_POST,discussPost.getId());
                myPost.put("likeCount",likeCount);
                myPostsVo.add(myPost);
            }
        }
        model.addAttribute("discussPosts",myPostsVo);
        model.addAttribute("totalRows",totalRows);
        return "/site/my-post";
    }
}
