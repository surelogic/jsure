package test;

/**
 * Tests that subanalysis objects and queries are properly used when switching
 * between the constructor and the instance initializer.  Best used with
 * the debugger looking at SimpleNonnullAnalysis.Transfer.createAnalysis(),
 * MustReleaseAnalysis.MustReleaseTransfer.transferIsObject(),
 * MustReleaseAnalysis.MustReleaseTransfer constructors,
 * MustHoldAnalysis.MustHoldTransfer.transferIsObject(),
 * MustHoldAnalysis.MustHoldTransfer constructors,
 * LockVisitor.getHeldJUCLocks()
 */
public class Test {
	{
		final C c = new C();
		
		// bad
		c.value = 1;
		
		// good
		c.lock.lock();
		try {
			c.value = 2;
		} finally {
			c.lock.unlock();
		}
	}
	
	@SuppressWarnings("null") public Test() {
		super();
		
		final C c = null;
		
		// bad
		c.value = 10;
		
		// good
		c.lock.lock();
		try {
			c.value = 20;
		} finally {
			c.lock.unlock();
		}
	}
	
	public void method() {
		final C c = new C();
		
		// bad
		c.value = 100;
		
		// good
		c.lock.lock();
		try {
			c.value = 200;
		} finally {
			c.lock.unlock();
		}
	}
}
