package com.heller.jmockit;

import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import org.junit.Assert;
import org.junit.Test;

//JMockit的程序结构
public class ProgramConstructureTest {
    
    // 这是一个测试属性
    @Mocked
    private HelloJMockit helloJMockit;
    
    @Test
    public void test1() {
        // 录制(Record)
        new Expectations() {{
            helloJMockit.sayHello();
            // 期待上述调用的返回是"hello,david"，而不是返回"hello,JMockit"
            result = "hello,david";
        }};
        // 重放(Replay)
        String msg = helloJMockit.sayHello();
        Assert.assertTrue(msg.equals("hello,david"));
        // 验证(Verification)
        new Verifications() {{
            helloJMockit.sayHello();
            times = 1;
        }};
    }
    
    /**
     * b)测试参数：即测试方法的参数。它仅作用于当前测试方法。
     * 给测试方法加参数，原本在JUnit中是不允许的，但是如果参数加了JMockit的注解API(@Mocked, @Tested, @Injectable,@Capturing)，则是允许的。
     */
    @Test
    public void test2(@Mocked HelloJMockit helloJMockit /* 这是一个测试参数 */) {
        // 录制(Record)
        new Expectations() {{
            helloJMockit.sayHello();
            // 期待上述调用的返回是"hello,david"，而不是返回"hello,JMockit"
            result = "hello,david";
        }};
        // 重放(Replay)
        String msg = helloJMockit.sayHello();
        Assert.assertTrue(msg.equals("hello,david"));
        // 验证(Verification)
        new Verifications() {{
            helloJMockit.sayHello();
            // 验证helloJMockit.sayHello()这个方法调用了1次
            times = 1;
        }};
    }
    
}
