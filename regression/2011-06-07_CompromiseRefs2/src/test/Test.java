package test;
import com.surelogic.RegionEffects;
import com.surelogic.Unique;

// Test StoreLattice.check() --- how to get an error in the middle of a method instead of at the end of the method
public class Test {
	private Other o;
	private int count;
	private int foo;
	
	@RegionEffects("none")
	private static void eatUnique(final Object p) {}

	public void m() {
		Other oo = this.o;
		Object t = oo.u;
		eatUnique(t);
		oo = null;
		count += 1;
		foo += 10;
	}

	public void m2() {
		{
			Other oo = this.o;
			Object t = oo.u;
			eatUnique(t);
		}
		count += 1;
		foo += 10;
	}
}


class Other {
	@Unique
	public Object u = null;
}
