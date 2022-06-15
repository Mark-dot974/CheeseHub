package com.mark.community.dao;

import com.mark.community.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;
// 使用springboot进行开发必须要使用Mapper表示该接口，因为Mapper注解表示Mybatis，而Repository表示Spring
@Mapper
public interface UserMapper {
    User selectById(int id);

    User selectByName(String username);

    User selectByEmail(String email);

    int insertUser(User user);

    int updateStatus(int id, int status);

    int updateHeader(int id, String headerUrl);

    int updatePassword(int id, String password);
}
