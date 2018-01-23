package com.heller.jmockit;

import java.util.Locale;

public class HelloJMockit {
    
    public String sayHello() {
        Locale locale = Locale.getDefault();
        if (locale.equals(Locale.CHINA)) {
            return "你好，JMockIt！";
        } else {
            return "Hello, JMockit!";
        }
    }
    
    // 静态方法
    public static int staticMethod() {
        return 1;
    }
    
    // 普通方法
    public int ordinaryMethod() {
        return 2;
    }
    
    // final方法
    public final int finalMethod() {
        return 3;
    }
    
    // native方法,返回4
//    public native int navtiveMethod();
    
    // private方法
    private int privateMethod() {
        return 5;
    }
    
    // 调用private方法
    public int callPrivateMethod() {
        return privateMethod();
    }
    
}
