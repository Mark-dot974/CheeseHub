package com.mark.community;

import com.mark.community.dao.DiscussPostMapper;
import com.mark.community.dao.LoginTicketMapper;
import com.mark.community.dao.UserMapper;
import com.mark.community.entity.LoginTicket;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Date;

@SpringBootTest
class CommunityApplicationTests {
	@Autowired
	UserMapper userMapper;
	@Autowired
	LoginTicketMapper loginTicketMapper;
	@Autowired
	DiscussPostMapper discussPostMapper;
	@Test
	void contextLoads() {
	}
	@Test
	void TestConnection(){
		//System.out.println(discussPostMapper.selectDiscussPosts(userId, 101,0,3,0 ));
	}
	@Test
	public void testTime(){
		System.out.println(new Date());
	}
	@Test
	public void testLoginTicket(){
		LoginTicket loginTicket = new LoginTicket();
		loginTicket.setUserId(101);
		loginTicket.setTicket("abc");
		loginTicket.setStatus(0);
		loginTicket.setExpired(new Date(System.currentTimeMillis() + 1000 * 60 * 10));

		loginTicketMapper.insertLoginTicket(loginTicket);
	}
}
