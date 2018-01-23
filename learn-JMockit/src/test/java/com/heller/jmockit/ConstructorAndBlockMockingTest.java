package com.heller.jmockit;

import mockit.Mock;
import mockit.MockUp;
import org.junit.Assert;
import org.junit.Test;

/**
 * Mock构造函数&初始代码块
 */
public class ConstructorAndBlockMockingTest {
    
    public static class AnOrdinaryClassWithBlockMockUp extends MockUp<AnOrdinaryClassWithBlock> {
        // Mock构造函数，函数名$init代表类的构造函数和普通的初始化块代码
        @Mock
        public void $init(int i) {
        }
        
        // Mock静态初始化块，函数名$clinit代表类的静态初始化块代码
        @Mock
        public void $clinit() {
        }
    }
    
    @Test
    public void testClassMockingByMockUp() {
        new AnOrdinaryClassWithBlockMockUp();
        AnOrdinaryClassWithBlock instance = new AnOrdinaryClassWithBlock(10);
        
        // 静态初始化块被Mock了
        Assert.assertTrue(AnOrdinaryClassWithBlock.j == 0);
        // 构造函数和初始化块代码被Mock了
        Assert.assertTrue(instance.getI() == 0);
    }
    
}
