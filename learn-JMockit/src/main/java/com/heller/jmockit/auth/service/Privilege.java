package com.heller.jmockit.auth.service;

//权限类，校验用户没有权限访问某资源
public interface Privilege {
    /**
     * 判断用户有没有权限
     *
     * @param userId
     * @return 有权限，就返回true,否则返回false
     */
    boolean isAllow(long userId);
    
}
