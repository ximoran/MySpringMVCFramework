package com.spring.dao.impl;

import com.spring.bean.UserVo;
import com.spring.dao.UserDao;
import com.spring.stereotype.MyRepository;

@MyRepository
public class UserDaoImpl implements UserDao {
    @Override
    public String getUserName(String username) {
        return "username:" + username;
    }

    @Override
    public UserVo getUserInfo(String name, Integer age, String sex) {

        UserVo userVo = new UserVo();
        userVo.setName(name);
        userVo.setAge(age);
        userVo.setSex(sex);
        return userVo;
    }
}
