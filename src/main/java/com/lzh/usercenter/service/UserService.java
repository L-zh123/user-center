package com.lzh.usercenter.service;

import com.lzh.usercenter.pojo.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lzh.usercenter.pojo.vo.SafetyUser;
import com.lzh.usercenter.utils.R;
import com.lzh.usercenter.vo.req.UserLoginReq;
import com.lzh.usercenter.vo.req.UserRegisterReq;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author Lenovo
* @description 针对表【user(用户表)】的数据库操作Service
* @createDate 2022-09-27 09:29:44
*/
public interface UserService extends IService<User> {

    /**
     *  用户注册
     * @param userInfo 用户注册信息
     * @return 新用户 id
     */
    R<Long> userRegister(UserRegisterReq userInfo);

    /**
     * 用户登录
     * @param userInfo 用户信息
     * @param request 请求
     * @return 用户侵袭
     */
    R<SafetyUser> userLogin(UserLoginReq userInfo, HttpServletRequest request);

    /**
     * 用户注销
     * @param request 请求
     * @return int类型
     */
    int userLogout(HttpServletRequest request);

    /**
     * 根据标签搜索用户
     * @param tags 用户的标签
     * @return 符合条件的集合
     */
    R<List<SafetyUser>> searchUsersByTags(List<String> tags);

    /**
     * 根据用户名查询
     * @param username 用户名
     * @return 用户列表
     */
    R<List<SafetyUser>> searchUserList(String username);

    /**
     * 用户删除
     * @param id 被删除用户id
     * @return 布尔值
     */
    R<Boolean> deleteUserById(long id);

    /**
     * 更新用户信息
     * @param user 用户参数
     * @return 布尔值
     */
    R<Boolean> updateUser(User user);

    /**
     * 推荐用户列表
     * @return 用户列表
     */
    R<List<SafetyUser>> recommendUsers(String redisKey);
}
