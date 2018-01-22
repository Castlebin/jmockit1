package com.heller.jmockit;

import java.util.Calendar;
import java.util.Locale;

import mockit.Mock;
import mockit.MockUp;
import org.junit.Assert;
import org.junit.Test;

//Mockup & @Mock的Mock方式
// MockUp & @Mock提供的Mock方式，程序员比较喜欢。因为它的Mock方式最直接。
public class MockUpTest {
    
    @Test
    public void testMockUp() {
        // 对Java自带类Calendar的get方法进行定制
        // 只需要把Calendar类传入MockUp类的构造函数即可
        new MockUp<Calendar>(Calendar.class) {
            // 想Mock哪个方法，就给哪个方法加上@Mock， 没有@Mock的方法，不受影响
            @Mock
            public int get(int unit) {
                if (unit == Calendar.YEAR) {
                    return 2017;
                }
                if (unit == Calendar.MONDAY) {
                    return 12;
                }
                if (unit == Calendar.DAY_OF_MONTH) {
                    return 25;
                }
                if (unit == Calendar.HOUR_OF_DAY) {
                    return 7;
                }
                return 0;
            }
        };
        // 从此Calendar的get方法，就沿用你定制过的逻辑，而不是它原先的逻辑。
        Calendar cal = Calendar.getInstance(Locale.FRANCE);
        Assert.assertTrue(cal.get(Calendar.YEAR) == 2017);
        Assert.assertTrue(cal.get(Calendar.MONDAY) == 12);
        Assert.assertTrue(cal.get(Calendar.DAY_OF_MONTH) == 25);
        Assert.assertTrue(cal.get(Calendar.HOUR_OF_DAY) == 7);
        // Calendar的其它方法，不受影响
        Assert.assertTrue((cal.getFirstDayOfWeek() == Calendar.MONDAY));
        
    }
    
}
/**
 * Mock方式是不是很简单，直接 ？ 难怪很多程序员们都喜欢用MockUp & @Mock了。
 *
 * 那是不是我们只需要掌握MockUp & @Mock就可以了？就不需要了解其它Mock API了？
 *
 * 当然不是！ 掌握了MockUp & @Mock的确能帮我们解决大部分的Mock场景，因为其使用方式最直接嘛。
 *
 * 比如下面的场景是MockUp & @Mock做不到的。
 *
 * 一个类有多个实例。只对其中某1个实例进行mock。
 * 最新版的JMockit已经让MockUp不再支持对实例的Mock了。1.19之前的老版本仍支持。
 *
 * AOP动态生成类的Mock。
 *
 * 对类的所有方法都需要Mock时，书写MockUp的代码量太大。
 * 比如web程序中，经常需要对HttpSession进行Mock。若用MockUp你要写大量的代码，可是用@Mocked就一行代码就可以搞定。
 *
 *
 *
 * MockUp & @Mock的确是好东西，在实际Mock场景中，我们需要灵活运用JMockit其它的Mock API。让我们的Mock程序简单，高效。
 *
 * MockUp & @Mock比较适合于一个项目中，用于对一些通用类的Mock，以减少大量重复的new Exceptations{{}}代码。
 */