package com.lzh.usercenter.job;

import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lzh.usercenter.pojo.User;
import com.lzh.usercenter.pojo.vo.SafetyUser;
import com.lzh.usercenter.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 缓存预热定时任务
 */
@Component
public class PreCacheJob {

    @Resource
    private RedisTemplate redisTemplate;
    @Resource
    private UserService userService;
    //重点用户
    private List<Long> mainUserList = Arrays.asList(1L);

    //每天执行
    @Scheduled(cron = "0 32 20 * * *")
    public void doCacheRecommendUser(){
        for (Long userId : mainUserList) {
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            List<User> list = userService.list(queryWrapper);
            String redisKey = String.format("friend:user:recommend:%s",userId);
            ArrayList<SafetyUser> safetyUserList = new ArrayList<>();
            for (User user : list) {
                SafetyUser safetyUser = new SafetyUser();
                //将标签字符串数组转换成JSON数组
                JSONArray tagsList = null;
                if(StringUtils.isNotBlank(user.getTags())){
                    tagsList= JSONArray.parseArray(user.getTags());
                }
                BeanUtils.copyProperties(user,safetyUser);
                safetyUser.setTags(tagsList);
                safetyUserList.add(safetyUser);
            }
            //从数据库查询完存入缓存
            redisTemplate.opsForValue().set(redisKey,safetyUserList,1, TimeUnit.DAYS);
        }

    }
}
