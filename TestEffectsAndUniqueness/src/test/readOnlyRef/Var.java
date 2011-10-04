package test.readOnlyRef;

import com.surelogic.Borrowed;
import com.surelogic.ReadOnly;
import com.surelogic.RegionEffects;
import com.surelogic.Unique;

public class Var {
	public int val;
	
	@Unique("return")
	@RegionEffects("none")
	public Var() {
		super();
	}
	
	@RegionEffects("reads Instance")
	@Borrowed("this")
	public int get() {
		return val;
	}
	
	@RegionEffects("writes Instance")
	@Borrowed("this")
	public void set(final int v) {
		val = v;
	}
	
	
	
	// ======================================================================
	// == Test @ReadOnly receiver	
	// ======================================================================
	
	/* Driver for the U+F analysis needs to be updated.  Need @Borrowed parameter
	 * here to force the analysis to check this method.
	 */
	@RegionEffects("none")
	@ReadOnly("this")
	public void testReceiverRead1(final @Borrowed Object o) {
		// Read effect should be degraded to "Reads Object:All"
		this.get();
	}
	
	/* Driver for the U+F analysis needs to be updated.  Need @Borrowed parameter
	 * here to force the analysis to check this method.
	 */
	@RegionEffects("none")
	@ReadOnly("this")
	public void testReceiverRead2(final @Borrowed Object o) {
		// Read effect should be degraded to "Reads Object:All"
		int z = this.val;
	}
	
	/* Driver for the U+F analysis needs to be updated.  Need @Borrowed parameter
	 * here to force the analysis to check this method.
	 */
	@RegionEffects("writes this:Instance")
	@ReadOnly("this")
	public void testReceiverWrite1(final int v, final @Borrowed Object o) {
		// CHECKED BY FLOW ANALYSIS: Write effect on immutable is illegal
		this.set(v);
	}	
	/* Driver for the U+F analysis needs to be updated.  Need @Borrowed parameter
	 * here to force the analysis to check this method.
	 */
	@RegionEffects("writes this:Instance")
	@ReadOnly("this")
	public void testReceiverWrite2(final int v, final @Borrowed Object o) {
		// CHECKED BY FLOW ANALYSIS: Write effect on immutable is illegal
		this.val = v;
	}
}
