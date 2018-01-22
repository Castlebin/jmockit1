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
    
}
