package com.spring.dao;

import com.spring.bean.UserVo;

public interface UserDao {

    String getUserName(String username);

    UserVo getUserInfo(String name, Integer age, String sex);
}
