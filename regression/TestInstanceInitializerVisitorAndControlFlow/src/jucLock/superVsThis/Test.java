package jucLock.superVsThis;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.surelogic.RegionLock;

@RegionLock("L is lock protects Instance")
public class Test {
	public final Lock lock = new ReentrantLock();
	
	@SuppressWarnings("unused")
  private int x = 10;
	
	{ 
		x = 100;
	}

	{
		lock.lock();
		try {
			x = 1000;
		} finally {
			lock.unlock();
		}
	}
	
	public Test(int v, int w) { /* super is implicit, visit initializers */ }
	
	public Test(int v) {
		super(); /* visit initializers */
	}
	
	public Test() {
		this(7); /* do not visit initializers */
	}
}
