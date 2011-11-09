package test;

import com.surelogic.RegionLock;

/**
 * Test that non-final lock expression warnings get linked to lock
 * promises.
 */

@RegionLock("L is this protects Instance")
public class C {
	
}

@RegionLock("L is this protects Instance")
class D {
	
}


class Test {
	private final C c1 = new C();
	private C c2;
	private final D d1 = new D();
	private D d2;
	
	
	public void doStuff(
			final C finalC, C nonFinalC,
			final Object finalO, Object nonFinalO) throws InterruptedException {
		
		synchronized (c1) {}
		
		synchronized (c2) {}
		
		synchronized (d1) {}
		
		synchronized (d2) {}
		
		synchronized (finalC) {}
		
		synchronized (nonFinalC) { nonFinalC = new C(); nonFinalC.wait(); }
		
		synchronized (finalO) {}
		
		synchronized (nonFinalO) { nonFinalO = new Object(); }
	}
}