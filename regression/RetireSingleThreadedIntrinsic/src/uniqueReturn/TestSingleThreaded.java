package uniqueReturn;

import com.surelogic.RegionEffects;
import com.surelogic.RegionLock;
import com.surelogic.Starts;
import com.surelogic.Unique;

@SuppressWarnings("unused")
@RegionLock("L is this protects Instance")
public class TestSingleThreaded {
	public Object alias;
	private int value1;
	private int value2;
	private int value3;
	private int value4;
	
	// Don't even try to be single threaded
	public TestSingleThreaded(String s) {
		value4 = 1;
	}
	
	@Unique("return")
	public TestSingleThreaded() {
		value1 = 0;
	}
	
	@Starts("nothing")
	@RegionEffects("none")
	public TestSingleThreaded(final int v) {
		value1 = v;
	}
	
	@Starts("nothing")
	@RegionEffects("none")
	@Unique("return")
	public TestSingleThreaded(final int v, final int w) {
		value1 = v;
		value2 = w;
	}
	
	@Starts("nothing")
	@RegionEffects("none")
	@Unique("return")  // violated by this one
	public TestSingleThreaded(final int v, final int w, final int z) {
		value1 = v;
		value2 = w;
		value3 = z;
		alias = this;
	}
	
	@Starts("nothing")// violated by this one
	@RegionEffects("none")
	@Unique("return")
	public TestSingleThreaded(final int v, final int w, final int z, final int zz) {
		value1 = v;
		value2 = w;
		value3 = z;
		value4 = zz;
		final Thread t = new Thread();
		t.start();
	}
}
