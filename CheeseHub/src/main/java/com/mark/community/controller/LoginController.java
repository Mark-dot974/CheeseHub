package com.mark.community.controller;

import com.google.code.kaptcha.Producer;
import com.mark.community.service.UserService;
import com.mark.community.util.CommunityConstant;
import com.mark.community.util.CommunityUtil;
import com.mark.community.util.RedisKeyUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.awt.image.BufferedImage;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Controller
public class LoginController {
    @Autowired
    private UserService userService;

    public final Logger logger= LoggerFactory.getLogger(LoginController.class);

    // 由Kaptcha配置类完成自动注入
    @Autowired
    private Producer captchaProducer;

    @Value("${server.servlet.context-path}")
    private String contextPath;
    @Autowired
    private RedisTemplate redisTemplate;

    @RequestMapping(value = "/login",method = RequestMethod.GET)
    public String getLoginPage(){
        return "/site/login";
    }

    @RequestMapping("/code")
    public void getCode(HttpServletResponse response, HttpSession session){
        // 生成验证码
        String text = captchaProducer.createText();
        BufferedImage image = captchaProducer.createImage(text);

        // 将验证码存到session中
        //session.setAttribute("code",text);

        // 把验证码存到redis中
        // 生成captcha拥有者的凭证存到cookie中（因为此时用户还没有登录，用这个来识别本次登录对应的验证码是那个）
        String captchaOwner = CommunityUtil.generateUUID();
        Cookie cookie = new Cookie("captchaOwner",captchaOwner);
        response.addCookie(cookie);

        // 存到redis中
        String captchaKey = RedisKeyUtil.getCaptchaKey(captchaOwner);
        // 验证码60s后自动过期删除、
        redisTemplate.opsForValue().set(captchaKey,text,60, TimeUnit.SECONDS);

        // 返回验证码图片给客户端
        response.setContentType("image/png");
        try{
            OutputStream outputStream = response.getOutputStream();
            ImageIO.write(image,"png",outputStream);
        }catch (Exception e){
            logger.error("生成验证码图片失败"+e.getMessage());
        }
    }
    @RequestMapping(path="/login",method = RequestMethod.POST)
    public String login(String username, String password, String code, boolean rememberme,
                        Model model,HttpSession session,HttpServletResponse response,@CookieValue("captchaOwner") String captchaOwner){
        // 检查验证码
//        String code_session = (String) session.getAttribute("code");
        //从redis中获取captcha
        String captchaKey = RedisKeyUtil.getCaptchaKey(captchaOwner);
        String code_session = (String) redisTemplate.opsForValue().get(captchaKey);
        if(StringUtils.isBlank(code_session)||StringUtils.isBlank(code) || !code.equalsIgnoreCase(code_session)){
            model.addAttribute("codeMsg","验证码不正确");
            return "/site/login";
        }
        // 检查账号，密码
        long expiredSeconds = rememberme? CommunityConstant.REMEMBER_EXPIRED_SECONDS:CommunityConstant.DEFAULT_EXPIRED_SECONDS;
        Map<String, Object> map = userService.login(username, password, expiredSeconds);
        // 如果map包含ticket，说明用户登录成功了。
        if(map.containsKey("ticket")){
            Cookie cookie = new Cookie("ticket", (String) map.get("ticket"));
            // 设置只有contextPath路径下的请求携带Cookie
            cookie.setPath(contextPath);
            cookie.setMaxAge((int) expiredSeconds);
            response.addCookie(cookie);
            return "redirect:/index";
        }
        // 登录不成功
        else{
            model.addAttribute("usernameMsg",map.get("usernameMsg"));
            model.addAttribute("passwordMsg",map.get("passwordMsg"));
            return "/site/login";
        }
    }
    @RequestMapping(path="/logout",method = RequestMethod.GET)
    public String logout(@CookieValue("ticket") String ticket){
     userService.logout(ticket);
        SecurityContextHolder.clearContext();
     return "redirect:/login";
    }

}
