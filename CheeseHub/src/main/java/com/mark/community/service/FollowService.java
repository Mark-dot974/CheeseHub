package com.mark.community.service;

import com.mark.community.entity.User;
import com.mark.community.util.CommunityConstant;
import com.mark.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class FollowService {
    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private UserService userService;

    public void follow(int userId , int entityType , int entityId){
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                //生成Key
                String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
                String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);
                operations.multi();
                // 在followeeKey这个可以对应的表中存入一个关注者（entityId），score是当前时间。
                operations.opsForZSet().add(followeeKey,entityId,System.currentTimeMillis());
                operations.opsForZSet().add(followerKey,userId,System.currentTimeMillis());

                return operations.exec();
            }
        });
    }

    public void unfollow(int userId , int entityType , int entityId){
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                //生成Key
                String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
                String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);
                operations.multi();
                // 在followeeKey这个可以对应的表中存入一个关注者（entityId），score是当前时间。
                operations.opsForZSet().remove(followeeKey,entityId);
                operations.opsForZSet().remove(followerKey,userId);

                return operations.exec();
            }
        });
    }

    // 查询关注的实体的数量
    public long findFolloweeCount(int userId , int entityType){
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
        return redisTemplate.opsForZSet().zCard(followeeKey);
    }

    // 查询实体粉丝的数量
    public long findFollowerCount(int entityType , int entityId){
        String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);
        return redisTemplate.opsForZSet().zCard(followerKey);
    }

    // 查询当前用户是否已经关注该实体
    public boolean hasFollowed(int userId , int entityType , int entityId){
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
        return redisTemplate.opsForZSet().score(followeeKey,entityId) != null ;
    }

    // 查询某用户关注的人,分页展示
    public List<Map<String,Object>> findFollowee(int userId,int offset , int limit){
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, CommunityConstant.ENTITY_TYPE_USER);
        Set<Integer> targetIds = redisTemplate.opsForZSet().reverseRange(followeeKey, offset, offset + limit - 1);
        if(targetIds == null){
            return null;
        }
        List<Map<String,Object>> list = new ArrayList<>();
        for (Integer targetId : targetIds) {
            Map<String,Object> map = new HashMap<>();
            User user = userService.findUserById(targetId);
            map.put("user",user);
            Double score = redisTemplate.opsForZSet().score(followeeKey, targetId);
            map.put("followTime",new Date(score.longValue()));
            list.add(map);
        }
        return list;
    }
    // 查询某用户的粉丝
    public List<Map<String,Object>> findFollowers(int userId , int offset , int limit){
        String followerKey = RedisKeyUtil.getFollowerKey(CommunityConstant.ENTITY_TYPE_USER, userId);
        //System.out.println("=====================key:"+followerKey);
        Set<Integer> targetIds = redisTemplate.opsForZSet().reverseRange(followerKey,offset,offset+limit-1);
        if(targetIds == null){
            return null;
        }
        List<Map<String,Object>> list = new ArrayList<>();
        for (Integer targetId : targetIds) {
            Map<String,Object> map = new HashMap<>();
            User user = userService.findUserById(targetId);
            map.put("user",user);
            //System.out.println(user.toString());
            Double score = redisTemplate.opsForZSet().score(followerKey, targetId);
            map.put("followTime",new Date(score.longValue()));
            list.add(map);
        }
        return list;
    }
}
