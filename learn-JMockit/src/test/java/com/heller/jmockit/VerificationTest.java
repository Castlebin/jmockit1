package com.heller.jmockit;

import java.util.Calendar;

import mockit.Expectations;
import mockit.Verifications;
import org.junit.Assert;
import org.junit.Test;

//Verification的使用
public class VerificationTest {
    
    @Test
    public void testVerification() {
        // 录制阶段
        Calendar cal = Calendar.getInstance();
        new Expectations(Calendar.class) {{
            // 对cal.get方法进行录制，并匹配参数 Calendar.YEAR
            cal.get(Calendar.YEAR);
            result = 2016;// 年份不再返回当前小时。而是返回2016年
            cal.get(Calendar.HOUR_OF_DAY);
            result = 7;// 小时不再返回当前小时。而是返回早上7点钟
        }};
        // 重放阶段
        Calendar now = Calendar.getInstance();
        Assert.assertTrue(now.get(Calendar.YEAR) == 2016);
        Assert.assertTrue(now.get(Calendar.HOUR_OF_DAY) == 7);
        // 验证阶段
        new Verifications() {{
            Calendar.getInstance();
            // 限定上面的方法只调用了1次，当然也可以不限定
            times = 1;
            cal.get(anyInt);
            // 限定上面的方法只调用了2次，当然也可以不限定
            times = 2;
        }};
    }
    
}
/**
 * 通常，在实际测试程序中，我们更倾向于通过JUnit/TestNG/SpringTest的Assert类对测试结果的验证， 对类的某个方法有没调用，调用多少次的测试场景并不是太多。因此在验证阶段，我们完全可以用JUnit/TestNG/SpringTest的Assert类取代new
 * Verifications() {{}}验证代码块。
 *
 * 除非，你的测试程序关心类的某个方法有没有调用，调用多少次，你可以使用new Verifications() {{}}验证代码块。
 *
 * 如果你还关心方法的调用顺序，你可以使用new VerificationsInOrder() {{}} .这里就不做详细的介绍了。
 */