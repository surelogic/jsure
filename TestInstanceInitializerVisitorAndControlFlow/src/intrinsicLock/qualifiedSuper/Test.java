package intrinsicLock.qualifiedSuper;

import com.surelogic.RegionLock;
import com.surelogic.Unique;

@RegionLock("LLL is this protects Instance")
public class Test extends Outer.Inner {
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
