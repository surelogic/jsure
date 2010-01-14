package intrinsicLock.superVsThis;

import com.surelogic.RegionLock;

@RegionLock("LLL is this protects Instance")
public class Test {
	@SuppressWarnings("unused")
  private int x = 10;
	
	{ 
		x = 100;
	}

	{
		synchronized (this) {
			x = 1000;
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
