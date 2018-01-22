package com.heller.jmockit;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import com.heller.jmockit.auth.service.Privilege;
import mockit.Capturing;
import mockit.Expectations;
import org.junit.Assert;
import org.junit.Test;

/**
 * @Capturing平时较少用到，但某些场景下，还非用它不可。 举个例子：通常我们的系统中，都有权限校验。我们通常用AOP来做权限校验，对不？可是AOP生成的类是哪个，连类名都不知道，还怎么Mock?
 * AOP生成的类是动态生成的。可是我们在单元测试时，不希望程序卡在权限校验上（除非是为了测试权限的测试程序）。这种情况下怎么办？用@Capturing !  看下面的例子！
 *
 * 加上了JMockit的API @Capturing, JMockit会帮我们实例化这个对象，它除了具有@Mocked的特点，还能影响它的*子类/实现类*
 */
//@Capturing注解用途
public class CapturingTest {
    
    // 测试用户ID
    long testUserId = 123456l;
    // 权限检验类，可能是人工写的
    Privilege privilegeManager1 = new Privilege() {
        @Override
        public boolean isAllow(long userId) {
            if (userId == testUserId) {
                return false;
            }
            return true;
        }
    };
    // 权限检验类，可能是JDK动态代理生成。我们通常AOP来做权限校验。
    Privilege privilegeManager2 = (Privilege)Proxy.newProxyInstance(Privilege.class.getClassLoader(),
        new Class[] {Privilege.class}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ((long)args[0] == testUserId) {
                    return false;
                }
                return true;
            }
        });
    
    // 有Cautring情形
    @Test
    public void testCaputring(@Capturing Privilege privilegeManager) {
        // 加上了JMockit的API @Capturing,
        // JMockit会帮我们实例化这个对象，它除了具有@Mocked的特点，还能影响它的子类/实现类
        new Expectations() {{
            // 对Privilege的所有实现类录制，假设测试用户有权限
            privilegeManager.isAllow(testUserId);
            result = true;
        }};
        // 不管权限校验的实现类是哪个，这个测试用户都有权限
        Assert.assertTrue(privilegeManager1.isAllow(testUserId));
        
        // 注意：jmockit 1.38版本有bug，对动态代理这种情况不支持！fuck！
        Assert.assertTrue(privilegeManager2.isAllow(testUserId));
    }
    
    // 没有Cautring情形
    @Test
    public void testWithoutCaputring() {
        // 不管权限校验的实现类是哪个，这个测试用户没有权限
        Assert.assertTrue(!privilegeManager1.isAllow(testUserId));
        Assert.assertTrue(!privilegeManager2.isAllow(testUserId));
    }
    
}
/**
 * 什么测试场景用@Capturing:
 *
 * 我们只知道父类或接口时，但我们需要控制它所有子类的行为时，子类可能有多个实现（可能有人工写的，也可能是AOP代理自动生成时）。就用@Capturing。
 */
