package com.heller.jmockit;

import java.util.Locale;

import mockit.Injectable;
import mockit.Mocked;
import org.junit.Assert;
import org.junit.Test;

//@Mocked与@Injectable的不同

/**
 * @Injectable 也是告诉 JMockit生成一个Mocked对象，但@Injectable只是针对其修饰的实例，而@Mocked是针对其修饰类的所有实例。
 * 此外，@Injectable对类的静态方法，构造函数没有影响。因为它只影响某一个实例嘛！
 */
public class MockedAndInjectable {
    
    @Test
    public void testMocked(@Mocked Locale locale) {
        // 静态方法不起作用了,返回了null
        Assert.assertTrue(Locale.getDefault() == null);
        // 非静态方法（返回类型为String）也不起作用了，返回了null
        Assert.assertTrue(locale.getCountry() == null);
        // 自已new一个，也同样如此，方法都被mock了
        Locale chinaLocale = new Locale("zh", "CN");
        Assert.assertTrue(chinaLocale.getCountry() == null);
    }
    
    @Test
    public void testInjectable(@Injectable Locale locale) {
        // 静态方法不mock
        Assert.assertTrue(Locale.getDefault() != null);
        // 非静态方法（返回类型为String）也不起作用了，返回了null,但仅仅限于locale这个对象
        Assert.assertTrue(locale.getCountry() == null);
        // 自已new一个，并不受影响
        Locale chinaLocale = new Locale("zh", "CN");
        Assert.assertTrue(chinaLocale.getCountry().equals("CN"));
    }
    
}
