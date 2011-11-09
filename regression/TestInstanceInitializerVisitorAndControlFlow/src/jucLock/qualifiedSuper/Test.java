package jucLock.qualifiedSuper;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.surelogic.RegionLock;
import com.surelogic.Unique;

@RegionLock("L is lock protects Instance")
public class Test extends Outer.Inner {
	public final Lock lock = new ReentrantLock();
	@SuppressWarnings("unused")
  private int f = 10;
	
	public Test(final Outer o) {
		o.super(); // should visit initialization of 'f'
	}
	
	@Unique("return")
	public Test(final Outer o, int x) {
		o.super(); // should visit initialization of 'f'
	}
	
	public Test() {
		this(new Outer()); // should NOT visit initialization of 'f'
	}
}
