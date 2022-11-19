package com.lzh.usercenter.controller;

import com.alibaba.fastjson.JSONArray;
import com.lzh.usercenter.pojo.User;
import com.lzh.usercenter.pojo.vo.SafetyUser;
import com.lzh.usercenter.service.UserService;
import com.lzh.usercenter.utils.R;
import com.lzh.usercenter.vo.req.TagsListReq;
import com.lzh.usercenter.vo.req.UserLoginReq;
import com.lzh.usercenter.vo.req.UserRegisterReq;
import io.swagger.annotations.Api;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

import static com.lzh.usercenter.contant.UserConstant.ADMIN_ROLE;
import static com.lzh.usercenter.contant.UserConstant.USER_LOGIN_STATE;
@Api(tags = "用户模块")
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    //用户注册
    @PostMapping("/register")
    public R<Long> userRegister(@RequestBody UserRegisterReq userInfo){
        if(userInfo == null){
            return R.error("注册信息不能为空");
        }
        return userService.userRegister(userInfo);
    }

    //用户登录
    @PostMapping("/login")
    public R<SafetyUser> userLogin(@RequestBody UserLoginReq userInfo, HttpServletRequest request){
        if(userInfo == null){
            return R.error("请输账号和密码");
        }
        return userService.userLogin(userInfo,request);
    }

    /**
     * 获取当前用户信息
     * @param request req
     * @return 用户最新信息
     */
    @GetMapping("/current")
    public R<SafetyUser> getCurrentUserInfo(HttpServletRequest request){
        //获取当前登录态
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        //强转
        SafetyUser user = (SafetyUser) userObj;
        if(user == null){
            return R.error("该用户登录已过期");
        }
        //根据id查询数据库中最新的数据
        User currentUser = userService.getById(user.getId());
        //将标签字符串数组转换成JSON数组
        JSONArray tagsList = JSONArray.parseArray(currentUser.getTags());
        //脱敏
        SafetyUser safetyUser = new SafetyUser();
        BeanUtils.copyProperties(currentUser,safetyUser);
        safetyUser.setTags(tagsList);
        return R.ok("获取当前用户信息成功",safetyUser);
    }

    //通过标签搜索用户
    @PostMapping("searchByTags")
    public R<List<SafetyUser>> searchUsersByTags(@RequestBody TagsListReq tagsListReq){
        System.out.println(tagsListReq.getTags());
        return userService.searchUsersByTags(tagsListReq.getTags());

    }

    //首页推荐
    @GetMapping("recommend")
    public R<List<SafetyUser>> recommendUsers(HttpServletRequest request){
        //获取用户信息
        SafetyUser user = getLoginUser(request);
        if(user == null){
            return R.error("请先登录");
        }
        String redisKey = String.format("friend:user:recommend:%s",user.getId());
        return userService.recommendUsers(redisKey);

    }

    //修改用户信息
    @PostMapping("/update")
    public R<Boolean> updateUser(@RequestBody User user,HttpServletRequest request){
        //1.校验参数是否为空
        if(user == null){
            return R.error("参数不能为空");
        }
        //2.仅用户自己和管理员可以修改
        SafetyUser loginUser = getLoginUser(request);
        if((isAdmin(request) && user.getId() > 0) || (loginUser != null && user.getId().equals(loginUser.getId()))){
            return userService.updateUser(user);
        }
        return R.error("修改失败");
    }

    //用户注销
    @PostMapping("/logout")
    public R<Integer> userLogout(HttpServletRequest request){
        if(request == null){
            return R.error("退出异常");
        }
        int result = userService.userLogout(request);
        return R.ok("退出成功",result);
    }

    //根据用户名模糊查询
    @GetMapping("/search")
    public R<List<SafetyUser>> searchUserList(String username,HttpServletRequest request){
        //仅管理员可查询
        if(!isAdmin(request)){
            return R.error("您没有权限!");
        }
        return userService.searchUserList(username);
    }

    //删除
    @DeleteMapping("/delete/{id}")
    public R<Boolean> deleteUserById(@PathVariable("id") long id,HttpServletRequest request){
        if(id<0){
            return R.error("删除失败");
        }
        //仅管理员可查询
        if(!isAdmin(request)){
            return R.error("您没有权限!");
        }
        return userService.deleteUserById(id);
    }

    private boolean isAdmin(HttpServletRequest request){
        //仅管理员可查询
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        SafetyUser user = (SafetyUser) userObj;
        // 判断是不是管理员
        return user != null && user.getUserRole() == ADMIN_ROLE;
    }

    private SafetyUser getLoginUser(HttpServletRequest request){
        if(request == null){
            return null;
        }
        //用户信息
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        SafetyUser user = (SafetyUser) userObj;
        return user;
    }
}
