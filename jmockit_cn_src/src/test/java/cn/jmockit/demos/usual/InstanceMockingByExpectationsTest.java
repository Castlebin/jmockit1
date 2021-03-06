package cn.jmockit.demos.usual;
/*
 * Copyright (c) jmockit.cn 
 * 访问JMockit中文网(jmockit.cn)了解该测试程序的细节
 */
import java.io.File;
import java.net.URI;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import cn.jmockit.demos.AnOrdinaryClass;
import mockit.Expectations;

//mock实例
public class InstanceMockingByExpectationsTest {
	@Test
	public void testInstanceMockingByExpectation() {
		AnOrdinaryClass instance = new AnOrdinaryClass();
		// 直接把实例传给Expectations的构造函数即可Mock这个实例
		new Expectations(instance) {
			{
				// 尽管这里也可以Mock静态方法，但不推荐在这里写。静态方法的Mock应该是针对类的
				// mock普通方法
				instance.ordinaryMethod();
				result = 20;
				// mock final方法
				instance.finalMethod();
				result = 30;
				// native, private方法无法用Expectations来Mock
			}
		};
		Assert.assertTrue(AnOrdinaryClass.staticMethod() == 1);
		Assert.assertTrue(instance.ordinaryMethod() == 20);
		Assert.assertTrue(instance.finalMethod() == 30);
		// 用Expectations无法mock native方法
		Assert.assertTrue(instance.navtiveMethod() == 4);
		// 用Expectations无法mock private方法
		Assert.assertTrue(instance.callPrivateMethod() == 5);
	}

	@BeforeClass
	// 加载AnOrdinaryClass类的native方法的dll
	public static void loadNative() throws Throwable {
		URI uri = ClassLoader.class.getResource("/").toURI();
		String realPath = new File(uri).getAbsolutePath() + "/libAnOrdinaryClass.dll";
		System.load(realPath);
	}
}
