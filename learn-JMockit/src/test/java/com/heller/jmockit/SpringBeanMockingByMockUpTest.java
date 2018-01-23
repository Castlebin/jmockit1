package com.heller.jmockit;

import java.io.File;
import java.net.URI;

import javax.annotation.Resource;

import mockit.Mock;
import mockit.MockUp;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

//用MockUp来Mock Spring Bean
@ContextConfiguration(locations = { "/applicationContext.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
public class SpringBeanMockingByMockUpTest {
    
    // 注入Spring Bean，Mock这个实例，就达到了Mock Spring Bean的目的
    @Resource
    HelloJMockit anOrdinaryBean;
    
    @Test
    public void testSpringBeanMockingByMockUp() {
        // 静态方法被mock了
        Assert.assertTrue(HelloJMockit.staticMethod() == 10);
        // 普通方法被mock了
        Assert.assertTrue(anOrdinaryBean.ordinaryMethod() == 20);
        // final方法被mock了
        Assert.assertTrue(anOrdinaryBean.finalMethod() == 30);
        // native方法被mock了
   //     Assert.assertTrue(anOrdinaryBean.navtiveMethod() == 40);
        // private方法被mock了
        Assert.assertTrue(anOrdinaryBean.callPrivateMethod() == 50);
    }
    
    @BeforeClass
    public static void beforeClassMethods() throws Throwable {
    //    loadNative();
        // 必须在Spring容器初始化前，就对Spring Bean的类做MockUp
        addMockUps();
    }
    
    // 加载AnOrdinaryClass类的native方法的dll
    public static void loadNative() throws Throwable {
        URI uri = ClassLoader.class.getResource("/").toURI();
        String realPath = new File(uri).getAbsolutePath() + "/libAnOrdinaryClass.dll";
        System.load(realPath);
    }
    
    // 对AnOrdinaryClass的Class
    public static class AnOrdinaryClassMockUp extends MockUp<HelloJMockit> {
        // Mock静态方法
        @Mock
        public static int staticMethod() {
            return 10;
        }
        
        // Mock普通方法
        @Mock
        public int ordinaryMethod() {
            return 20;
        }
        
        @Mock
        // Mock final方法
        public final int finalMethod() {
            return 30;
        }
        
        // Mock native方法
        @Mock
        public int navtiveMethod() {
            return 40;
        }
        
        // Mock private方法
        @Mock
        private int privateMethod() {
            return 50;
        }
    }
    
    // 添加MockUp
    public static void addMockUps() {
        new AnOrdinaryClassMockUp();
    }
    
}
