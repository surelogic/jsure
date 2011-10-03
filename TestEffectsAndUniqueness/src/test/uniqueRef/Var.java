package test.uniqueRef;

import com.surelogic.Borrowed;
import com.surelogic.InRegion;
import com.surelogic.Region;
import com.surelogic.RegionEffects;
import com.surelogic.Regions;
import com.surelogic.Unique;

@Regions({
	@Region("public V1"),
	@Region("public V2")
})
public class Var {
	@InRegion("V1")
	private int val1;
	
	@InRegion("V2")
	private int val2;
	
	
	
	@Unique("return")
	@RegionEffects("none")
	public Var() {
		super();
	}

	
	
	@RegionEffects("reads V1")
	@Borrowed("this")
	public int get1() {
		return val1;
	}
	
	@RegionEffects("writes V1")
	@Borrowed("this")
	public void set1(final int v) {
		val1 = v;
	}

	@RegionEffects("reads V2")
	@Borrowed("this")
	public int get2() {
		return val2;
	}
	
	@RegionEffects("writes V2")
	@Borrowed("this")
	public void set2(final int v) {
		val2 = v;
	}
//
//	
//	
//	// ======================================================================
//	// == Test @Immutable receiver	
//	// ======================================================================
//	
//	/* Driver for the U+F analysis needs to be updated.  Need @Borrowed parameter
//	 * here to force the analysis to check this method.
//	 */
//	@RegionEffects("none")
//	@Immutable("this")
//	public void testReceiverRead(final @Borrowed Object o) {
//		// Read effect on immutable should be discarded
//		this.get();
//	}
//	
//	/* Driver for the U+F analysis needs to be updated.  Need @Borrowed parameter
//	 * here to force the analysis to check this method.
//	 */
//	@RegionEffects("writes this:Instance")
//	@Immutable("this")
//	public void testReceiverWrite(final int v, final @Borrowed Object o) {
//		// CHECKED BY FLOW ANALYSIS: Write effect on immutable is illegal
//		this.set(v);
//	}
}
