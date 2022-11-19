package com.lzh.usercenter.vo.req;

import lombok.Data;

/**
 *用户注册请求体
 */
@Data
public class UserRegisterReq {
    private String userAccount;
    private String userPassword;
    private String checkPassword;
}
