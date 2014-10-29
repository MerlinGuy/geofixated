package org.geof.service;

import static org.junit.Assert.*;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.geof.test.TestRunnable;
import org.junit.Test;

public class PriviledgedThreadFactoryTest {

	@Test
	public void test() {
		try {
			ExecutorService _executor = null;
			ThreadFactory factory = DaemonThreadFactory.getPriviledgedFactory();
//			ThreadFactory factory = new DaemonThreadFactory();
			_executor = Executors.newFixedThreadPool(4, factory);
			TestRunnable tr = new TestRunnable();
			
			_executor.execute(tr);
			while (tr.isRunning()) {
				Thread.sleep(2000);
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail("Not yet implemented");
		}

	}

}
