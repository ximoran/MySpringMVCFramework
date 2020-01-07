package com.spring.service.impl;

import com.spring.bean.UserVo;
import com.spring.dao.UserDao;
import com.spring.service.UserService;
import com.spring.stereotype.MyAutowired;
import com.spring.stereotype.MyService;

@MyService
public class UserServiceImpl implements UserService {

    @MyAutowired
    private UserDao userDao;

    @Override
    public String getUserName(String username) {
        return userDao.getUserName(username);
    }

    @Override
    public UserVo getUserInfo(String name, Integer age, String sex) {
        return userDao.getUserInfo(name, age, sex);
    }
}
