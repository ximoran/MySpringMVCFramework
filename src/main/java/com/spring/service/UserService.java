package com.spring.service;

import com.spring.bean.UserVo;

public interface UserService {

    String getUserName(String username);

    UserVo getUserInfo(String name, Integer age, String sex);
}
