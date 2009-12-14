package borrowedThis;

import com.surelogic.Borrowed;
import com.surelogic.RegionEffects;
import com.surelogic.SingleThreaded;
import com.surelogic.Starts;

public class TestSingleThreaded {
	public Object alias;
	private int value;
	
	@SingleThreaded
	@Borrowed("this")
	public TestSingleThreaded() {
		value = 0;
	}
	
	@SingleThreaded
	@Starts("nothing")
	@RegionEffects("none")
	public TestSingleThreaded(final int v) {
		value = v;
	}
	
	@SingleThreaded
	@Starts("nothing")
	@RegionEffects("none")
	@Borrowed("this")
	public TestSingleThreaded(final int v, final int w) {
		value = v + w;
	}
	
	@SingleThreaded
	@Starts("nothing")
	@RegionEffects("none")
	@Borrowed("this") // violated by this one
	public TestSingleThreaded(final int v, final int w, final int z) {
		value = v + w + z;
		alias = this;
	}
	
	@SingleThreaded
	@Starts("nothing")// violated by this one
	@RegionEffects("none")
	@Borrowed("this") 
	public TestSingleThreaded(final int v, final int w, final int z, final int zz) {
		value = v + w + z + zz;
		final Thread t = new Thread();
		t.start();
	}
}
