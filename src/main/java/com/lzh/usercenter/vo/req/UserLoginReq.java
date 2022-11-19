package com.lzh.usercenter.vo.req;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户登录请求体
 *
 */
@Data
public class UserLoginReq implements Serializable {

    private static final long serialVersionUID = 3191241716373120793L;

    private String userAccount;

    private String userPassword;
}
