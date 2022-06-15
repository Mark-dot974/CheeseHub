package com.mark.community.service;

import com.mark.community.dao.LoginTicketMapper;
import com.mark.community.dao.UserMapper;
import com.mark.community.entity.LoginTicket;
import com.mark.community.entity.User;
import com.mark.community.util.CommunityConstant;
import com.mark.community.util.CommunityUtil;
import com.mark.community.util.MailClient;
import com.mark.community.util.RedisKeyUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.*;

@Service
public class UserService {
    @Autowired
    private UserMapper userMapper;

//    @Autowired
//    private LoginTicketMapper loginTicketMapper;

    @Autowired
    private MailClient mailClient;

    @Autowired
    private TemplateEngine templateEngine;

    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    private RedisTemplate redisTemplate;

    public User findUserById(int id){
        // 如果用户更新了用户信息，因为清除了缓存，可能getUser时为空
        User user = getCache(id);
        if(user == null){
            initCache(id);
            user = getCache(id);
        }
        return user;
    }

    public Map<String,Object> register(User user){
        Map<String,Object> map = new HashMap<>();
        // 空值处理
        if(user == null)
        {
            throw new IllegalArgumentException("参数不能为空！");
        }
        if(StringUtils.isBlank(user.getUsername()))
        {
            map.put("usernameMsg","账号不能为空");
            return map;
        }
        if (StringUtils.isBlank(user.getPassword())) {
            map.put("passwordMsg", "密码不能为空!");
            return map;
        }
        if (StringUtils.isBlank(user.getEmail())) {
            map.put("emailMsg", "邮箱不能为空!");
            return map;
        }
        // 验证账号，分别根据账号、邮箱查找当前账号是否已经存在
        User u = userMapper.selectByName(user.getUsername());
        if(u!=null){
            map.put("usernameMsg","该账号已经存在！");
            return map;
        }
        u = userMapper.selectByEmail(user.getEmail());
        if(u!=null)
        {
            map.put("emailMsg","该邮箱已经被注册");
            return map;
        }

        // 开始注册
        user.setSalt(CommunityUtil.generateUUID().substring(0,5));
        user.setPassword(CommunityUtil.md5(user.getPassword()+user.getSalt()));
        user.setType(0);
        user.setStatus(0);
        user.setActivationCode(CommunityUtil.generateUUID());
        user.setHeaderUrl(String.format("http://images.nowcoder.com/head/%dt.png", new Random().nextInt(1000)));
        user.setCreateTime(new Date());
        userMapper.insertUser(user);

        // 发送激活邮件
        // 前端页面的上下文，用于存取变量
        Context context = new Context();
        context.setVariable("email", user.getEmail());
        // http://localhost:8080/community/activation/101/code
        String url = domain + contextPath + "/activation/" + user.getId() + "/" + user.getActivationCode();
        context.setVariable("url", url);
        String content = templateEngine.process("/mail/activation", context);
        mailClient.sendMail(user.getEmail(), "激活账号", content);
        return map;
    }

    public int activation(int userId, String code) {
        // 先根据userId查找注册的用户信息
        User user = userMapper.selectById(userId);
        if(user == null){
            return 0;
        }
        if (user.getStatus() == 1) {
            return CommunityConstant.ACTIVATION_REPEAT;
        } else if (user.getActivationCode().equals(code)) {
            userMapper.updateStatus(userId, 1);
            return CommunityConstant.ACTIVATION_SUCCESS;
        } else {
            return CommunityConstant.ACTIVATION_FAILURE;
        }
    }
    public Map<String, Object> login(String username, String password, long expiredSeconds){
        // 验证客户端传递的信息是否可用
        Map<String,Object> map = new HashMap<>();
        if(StringUtils.isBlank(username)){
            map.put("usernameMsg","账号不能为空！");
            return map;
        }
        if(StringUtils.isBlank(password))
        {
            map.put("passwordMsg","密码不能为空！");
            return map;
        }

        // 验证账号
        User user = userMapper.selectByName(username);
        if(user==null){
            map.put("usernameMsg","该账号不存在！");
            return map;
        }

        // 验证状态(激活 or 未激活)
        if(user.getStatus()==0){
            map.put("usernameMsg","该账号未激活！");
            return map;
        }

        // 验证密码
        String encodedPassword = CommunityUtil.md5(password+user.getSalt());
        if(!encodedPassword.equals(user.getPassword())){
            map.put("passwordMsg","密码不正确！");
            return map;
        }

        // 验证通过，登录成功，记录登录状态
        LoginTicket loginTicket = new LoginTicket();
        loginTicket.setUserId(user.getId());
        loginTicket.setStatus(0);
        loginTicket.setTicket(CommunityUtil.generateUUID());
        loginTicket.setExpired(new Date(System.currentTimeMillis()+expiredSeconds*1000));
        String ticketKey = RedisKeyUtil.getTicketKey(loginTicket.getTicket());
        redisTemplate.opsForValue().set(ticketKey,loginTicket);
        initCache(user.getId());
        // loginTicketMapper.insertLoginTicket(loginTicket);
        map.put("ticket",loginTicket.getTicket());
        return map;
    }

    // 退出登录只是修改登录凭证的状态，防止未来需求需要，如统计用户登录次数
    public void logout(String ticket) {
        String ticketKey = RedisKeyUtil.getTicketKey(ticket);
        LoginTicket t = (LoginTicket) redisTemplate.opsForValue().get(ticketKey);
        t.setStatus(1);
        redisTemplate.opsForValue().set(ticketKey,t);
        clearCache(t.getUserId());
       // loginTicketMapper.updateStatus(ticket,1);
    }

    public LoginTicket findLoginTicket(String ticket){
        String ticketKey = RedisKeyUtil.getTicketKey(ticket);
        return (LoginTicket) redisTemplate.opsForValue().get(ticketKey);
    }

    public void updateHeader(int id, String headUrl) {
        userMapper.updateHeader(id,headUrl);
        // 更新用户信息后要清除缓存，防止信息不一致。
        clearCache(id);
    }

    public void editPassword(int id, String newPassword) {
        userMapper.updatePassword(id,newPassword);
        clearCache(id);
    }

    public User findUserByName(String toName) {
        return userMapper.selectByName(toName);
    }

    // 第一次查询，将查询内容放到redis中
    public void initCache(int userId){
        User user = userMapper.selectById(userId);
        String userKey = RedisKeyUtil.getUserKey(userId);
        redisTemplate.opsForValue().set(userKey,user);
    }
    public void clearCache(int userId){
        String userKey = RedisKeyUtil.getUserKey(userId);
        redisTemplate.delete(userKey);
    }
    public User getCache(int userId){
        String userKey = RedisKeyUtil.getUserKey(userId);
        return (User) redisTemplate.opsForValue().get(userKey);
    }

    // 获取用户的权限
    public Collection<? extends GrantedAuthority> getAuthorities(int userId){
        User user = this.findUserById(userId);

        List<GrantedAuthority> list = new ArrayList<>();
        list.add(new GrantedAuthority() {
            @Override
            public String getAuthority() {
                switch (user.getType()){
                    case 1:
                        return CommunityConstant.AUTHORITY_ADMIN;
                    case 2:
                        return CommunityConstant.AUTHORITY_MODERATOR;
                    default:
                        return CommunityConstant.AUTHORITY_USER;
                }
            }
        });
        return list;
    }
}
