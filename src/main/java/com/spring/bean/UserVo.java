package com.spring.bean;

import lombok.Data;

@Data
public class UserVo {
    private String name;
    private Integer age;
    private String sex;

    @Override
    public String toString() {
        return "UserVo{" +
                "name='" + name + '\'' +
                ", age=" + age +
                ", sex='" + sex + '\'' +
                '}';
    }
}
