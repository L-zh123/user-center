package com.lzh.usercenter.vo.req;

import lombok.Data;

import java.util.List;

@Data
public class TagsListReq {
    //搜索用户的标签
    private List<String> tags;
}
