package com.heller.jmockit;

import java.util.Locale;

import mockit.Expectations;
import org.junit.Assert;
import org.junit.Test;

public class HelloJMockitTest {
    
    /**
     * 测试场景：当前是在中国
     */
    @Test
    public void testSayHelloAtChina() {
        new Expectations(Locale.class) {{
            Locale.getDefault();
            result = Locale.CHINA;
        }};
        Assert.assertTrue("你好，JMockIt！".equals(new HelloJMockit().sayHello()));
    }
    
    /**
     * 测试场景：当前是在美国
     */
    @Test
    public void testSayHelloAtUS() {
        new Expectations(Locale.class) {{
            Locale.getDefault();
            result = Locale.US;
        }};
        Assert.assertTrue("Hello, JMockit!".equals(new HelloJMockit().sayHello()));
    }
    
}
