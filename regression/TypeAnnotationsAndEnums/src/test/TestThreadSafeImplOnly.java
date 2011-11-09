package test;

import com.surelogic.RegionLock;
import com.surelogic.ThreadSafe;
import com.surelogic.Unique;

@ThreadSafe(implementationOnly=true)
@RegionLock("L is this protects Instance")
public enum TestThreadSafeImplOnly {
	A(0, 1, 2) {
		private NotThreadSafe f = null;
	},
	
	B(1, 2, 3) {
		private final int f = 0;
		private Safe s = new Safe();
	},
	
	C(2, 3, 4) {
		public final int f = 10;
	},
	
	D(10, 11, 12);
	
	private final int x;
	private volatile int y;
	private int z;
	
	@Unique("return")
	private TestThreadSafeImplOnly(final int a, final int b, final int c) {
		x = a;
		y = b;
		z = c;
	}
	
	public synchronized int get() {
		return x + y + z;
	}
}

