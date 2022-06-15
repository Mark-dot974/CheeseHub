package com.mark.community;

import com.mark.community.util.MailClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.thymeleaf.TemplateEngine;

import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

//@RunWith(SpringRunner.class)
//@SpringBootTest
//@ContextConfiguration(classes = CommunityApplication.class)
public class TestMail {
    @Autowired
    private MailClient mailClient;
    @Autowired
    private TemplateEngine templateEngine;
    @Test
    public void testTextMail(){
        mailClient.sendMail("2449333189@qq.com","Test","Welcome");
    }

    @Test
    public void testDate(){
        System.out.println("aaa".getBytes());
    }

}
