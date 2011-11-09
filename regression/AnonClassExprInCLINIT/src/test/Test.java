package test;

import com.surelogic.Borrowed;
import com.surelogic.RegionEffects;
import com.surelogic.Unique;
import com.surelogic.UniqueInRegion;

public class Test {

	@UniqueInRegion("All")
	private static final D d = new D();

	private class Inner {
		// blah
	}

	static {
		/* Used to get a "no 'this' in scope" error here because analysis
		 * erroneously assumed that we were in a non-static context.  Now this
		 * should assure just fine.
		 */
		final Inner i = new Test().new Inner() {
		};
	}
}

class D {
	@Unique("return")
	public D() {
	}

	@RegionEffects("writes Instance")
	@Borrowed("this")
	public void m() {
	}
}