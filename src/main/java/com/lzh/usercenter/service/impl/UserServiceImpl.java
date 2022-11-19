package com.lzh.usercenter.service.impl;
import java.util.ArrayList;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lzh.usercenter.pojo.User;
import com.lzh.usercenter.pojo.vo.SafetyUser;
import com.lzh.usercenter.service.UserService;
import com.lzh.usercenter.mapper.UserMapper;
import com.lzh.usercenter.utils.R;
import com.lzh.usercenter.vo.req.UserLoginReq;
import com.lzh.usercenter.vo.req.UserRegisterReq;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.lzh.usercenter.contant.UserConstant.USER_LOGIN_STATE;

/**
* @author Lenovo
* @description 针对表【user(用户表)】的数据库操作Service实现
* @createDate 2022-09-27 09:29:44
*/
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{

    @Resource
    private PasswordEncoder passwordEncoder;
    @Resource
    private UserMapper userMapper;
    @Resource
    private RedisTemplate redisTemplate;

    @Override
    public R<Long> userRegister(UserRegisterReq userInfo) {
        String userAccount = userInfo.getUserAccount();
        String userPassword = userInfo.getUserPassword();
        String checkPassword = userInfo.getCheckPassword();
        //1.校验
        if(StringUtils.isAnyBlank(userAccount,userPassword,checkPassword)){
            return R.error("用户信息不能为空");
        }
        //2.1 账号不小于4位
        if(userAccount.length() < 4){
            return R.error("账号不能小于4位");
        }
        //2.2.密码不小于8位
        if(userPassword.length() < 8 || checkPassword.length() < 8){
            return R.error("密码不能小于8位");
        }
        //2.3.账户不能包含特殊字符
        String validPattern = "[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if(matcher.find()){
            //账户特殊包含字符
            return R.error("账号不能包含特殊字符");
        }

        //2.5.密码与校验密码相同
        if(!userPassword.equals(checkPassword)){
            //两次密码不相同
            return R.error("两次密码不相同");
        }
        //2.6.密码加密
        String encodePassword = passwordEncoder.encode(userPassword);
        //2.7.账户不能重复---(涉及数据库查询放到后面进行)
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount",userAccount);
        long count = this.count(queryWrapper);
        if(count > 0){
            //有人注册了
            return R.error("该账号已被注册");
        }
        //2.8.插入用户数据
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encodePassword);
        boolean saveResult = this.save(user);
        if(!saveResult){
            //保存失败
            return R.error("注册失败,请重试");
        }
        //注册成功 -- 返回新用户id
        return R.ok("注册成功",user.getId());
    }

    @Override
    public R<SafetyUser> userLogin(UserLoginReq userInfo, HttpServletRequest request) {
        String userAccount = userInfo.getUserAccount();
        String userPassword = userInfo.getUserPassword();

        //1.校验
        if(StringUtils.isAnyBlank(userAccount,userPassword)){
            return R.error("账号或密码不能为空");
        }
        //2.1 账号不小于4位
        if(userAccount.length() < 4){
            return R.error("账号不小于4位");
        }
        //2.2.密码不小于8位
        if(userPassword.length() < 8){
            return R.error("密码不小于8位");
        }
        //2.3.账户不能包含特殊字符
        String validPattern = "[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if(matcher.find()){
            //账户特殊包含字符
            return R.error("账号包含特殊字符");
        }
        //2.4.根据账号查询用户
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount",userAccount);
        User user = this.getOne(queryWrapper);
        if(user == null){
            //没查到
            return R.error("该账号不存在");
        }
        //2.5.检验密码是否相同
        boolean matches = passwordEncoder.matches(userPassword, user.getUserPassword());
        if(!matches){
            // 密码错误
            return R.error("密码错误,请重试");
        }

        //2.5.用户脱敏
        SafetyUser safetyUser = new SafetyUser();
        BeanUtils.copyProperties(user,safetyUser);
        //3. 记录用户的登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, safetyUser);

        return R.ok("登陆成功",safetyUser);
    }

    /**
     * 用户注销
     * @param request 请求
     * @return
     */
    @Override
    public int userLogout(HttpServletRequest request) {
        //移除登录态
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return 1;
    }

    /**
     * 根据标签搜索用户
     * @param tagList 用户的标签
     * @return 符合条件的集合
     */
    @Override
    public R<List<SafetyUser>> searchUsersByTags(List<String> tagList){
        if(CollectionUtils.isEmpty(tagList)){
            return R.error("用户标签不能为空");
        }
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        //拼接and查询
        for (String tagName : tagList) {
            queryWrapper = queryWrapper.like("tags",tagName);
        }
        List<User> list = this.list(queryWrapper);
        ArrayList<SafetyUser> safetyUserList = new ArrayList<>();
        for (User user : list) {
            SafetyUser safetyUser = new SafetyUser();
            //将标签字符串数组转换成JSON数组
            JSONArray tagsList = JSONArray.parseArray(user.getTags());
            BeanUtils.copyProperties(user,safetyUser);
            safetyUser.setTags(tagsList);
            safetyUserList.add(safetyUser);
        }
//        System.out.println("safetyUsersList = " + safetyUserList);
        return R.ok(safetyUserList);
    }

    /**
     * 根据用户名查询
     * @param username 用户名
     * @return 用户列表
     */
    @Override
    public R<List<SafetyUser>> searchUserList(String username) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        if(StringUtils.isNotBlank(username)){
            queryWrapper.like("username",username);
        }
        List<User> userList = this.list(queryWrapper);
        //用户脱敏
        List<SafetyUser> safetyUserList = new ArrayList<>();
        for (User user : userList) {
            SafetyUser safetyUser = new SafetyUser();
            BeanUtils.copyProperties(user,safetyUser);
            safetyUserList.add(safetyUser);
        }
        return R.ok("查询成功",safetyUserList);
    }

    /**
     * 用户删除
     * @param id 被删除用户id
     * @return 布尔值
     */
    @Override
    public R<Boolean> deleteUserById(long id) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id",id);
        boolean removeResult = this.remove(queryWrapper);
        if(!removeResult){
            return R.error("删除失败");
        }
        return R.ok("删除成功");
    }

    /**
     * 更新用户信息
     * @param user 用户参数
     * @return 布尔值
     */
    @Override
    public R<Boolean> updateUser(User user) {
        long userId = user.getId();
        //通过Id查询用户信息
        User oldUser = userMapper.selectById(userId);
        if(oldUser == null){
            return R.error("该用户不存在!");
        }
        //更新用户
        int i = userMapper.updateById(user);
        return i == 1 ? R.ok("更新成功",true) : R.error("更新失败");
    }

    /**
     * 推荐用户列表
     * @return 用户列表
     */
    @Override
    public R<List<SafetyUser>> recommendUsers(String redisKey) {
        //先从缓存中查询
        //如果有缓存，查缓存
        List<SafetyUser> users = (List<SafetyUser>)redisTemplate.opsForValue().get(redisKey);
        if(users != null){
            return R.ok("查询成功",users);
        }
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        List<User> list = this.list(queryWrapper);
        System.out.println("list = " + list);
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
        redisTemplate.opsForValue().set(redisKey,safetyUserList,1, TimeUnit.HOURS);
        return R.ok("查询成功",safetyUserList);
    }

}




