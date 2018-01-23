package com.heller.jmockit;

import java.util.Calendar;
import java.util.Date;

import mockit.Invocation;
import mockit.Mock;
import mockit.MockUp;
import org.junit.Assert;
import org.junit.Test;

// 可以在Mock中调用原始方法
public class InvocationMockUpTest {
    
    @Test
    public void testMockUp() {
        // 对Java自带的Calendar的get方法进行Mock
        new MockUp<Calendar>(Calendar.class) {
            // 方法申明中的Invocation对象，表示原始方法的调用
            @Mock
            public int get(Invocation invocation, int unit) {
                // mock部分
                if (unit == Calendar.HOUR_OF_DAY) {
                    return 7;
                }
                
                // 其他调用均保持是对原始方法的调用
                return invocation.proceed(unit);
            }
        };
        
        Calendar now = Calendar.getInstance();
        // 只有小时变成Mock方法
        Assert.assertTrue(now.get(Calendar.HOUR_OF_DAY) == 7);
        // 其他的都是保持对原始方法的调用行为
        Assert.assertTrue(now.get(Calendar.MONTH) == new Date().getMonth());
        Assert.assertTrue(now.get(Calendar.DAY_OF_MONTH) == new Date().getDate());
    }
    
}
