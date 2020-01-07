package com.spring.controller;

import com.spring.bean.UserVo;
import com.spring.service.UserService;
import com.spring.stereotype.MyAutowired;
import com.spring.stereotype.MyController;
import com.spring.stereotype.MyRequestMapping;
import com.spring.stereotype.MyRequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@MyController
@MyRequestMapping(value = "/user")
public class UserController {

    @MyAutowired
    private UserService userService;

    @MyRequestMapping("/getUserName")
    public void getUserName(HttpServletRequest request, HttpServletResponse response, @MyRequestParam("name") String name
            ,@MyRequestParam("age") Integer age,@MyRequestParam("sex") String sex) throws IOException {
        response.getWriter().write(userService.getUserName(name));
    }

    @MyRequestMapping("/getUserInfo")
    public void getUserInfo(HttpServletRequest request, HttpServletResponse response, @MyRequestParam("name") String name
            ,@MyRequestParam("age") Integer age,@MyRequestParam("sex") String sex) throws IOException {

        final UserVo userInfo = userService.getUserInfo(name, age, sex);

        response.getWriter().write(userInfo.toString());
    }
}
