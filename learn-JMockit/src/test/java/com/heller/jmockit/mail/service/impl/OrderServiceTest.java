package com.heller.jmockit.mail.service.impl;

import com.heller.jmockit.mall.service.MailService;
import com.heller.jmockit.mall.service.UserCheckService;
import com.heller.jmockit.mall.service.impl.OrderServiceImpl;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import org.junit.Assert;
import org.junit.Test;

/**
 * @Tested和@Injectable搭配使用
 */
public class OrderServiceTest {
    
    //@Tested修饰的类，表示是我们要测试对象,在这里表示，我想测试订单服务类。JMockit也会帮我们实例化这个测试对象
    @Tested
    private OrderServiceImpl orderService;
    
    // 测试注入方式
    @Test
    public void testSubmitOrder(@Injectable MailService mailService,
                                @Injectable UserCheckService userCheckService) {
        //测试用户ID
        long testUserId = 123456L;
        //测试商品id
        long testItemId = 456789L;
        
        new Expectations() {
            {
                // 当向testUserId发邮件时，假设都发成功了
                mailService.sendMail(testUserId, anyString);
                result = true;
                
                // 当检验testUserId的身份时，假设该用户都是合法的
                userCheckService.check(testUserId);
                result = true;
            }
        };
        // JMockit帮我们实例化了mailService了，并通过OrderService的构造函数，注入到orderService对象中。
        // JMockit帮我们实例化了userCheckService了，并通过OrderService的属性，注入到orderService对象中。
        Assert.assertTrue(orderService.submitOrder(testUserId, testItemId));
    }
    
}
/**
 * @Tested & @Injectable功能总结:
 * @Injectable 也表示一个Mocked对象，相比@Mocked，只不过只影响类的一个实例。而@Mocked默认是影响类的所有实例。
 * @Tested表示被测试对象。如果该对象没有赋值，JMockit会去实例化它，若@Tested的构造函数有参数， 则JMockit通过在测试属性&测试参数中查找@Injectable修饰的Mocked对象注入@Tested对象的构造函数来实例化，
 * 不然，则用无参构造函数来实例化。除了构造函数的注入，JMockit还会通过属性查找的方式，把@Injectable对象注入到@Tested对象中。
 * 注入的匹配规则：先类型，再名称(构造函数参数名，类的属性名)。若找到多个可以注入的@Injectable，则选择最优先定义的@Injectable对象。
 * 当然，我们的测试程序要尽量避免这种情况出现。因为给哪个测试属性/测试参数加@Injectable，是人为控制的。
 *
 *
 *
 * 什么测试场景，我们要使用@Tested & @Injectable:
 *
 * 显然，当我们需要手工管理被测试类的依赖时,就需要用到@Tested & @Injectable。
 * 两者搭配起来用，JMockit就能帮我们轻松搞定被测试类及其依赖注入细节。
 */