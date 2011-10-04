package test.immutableRef.array;


import com.surelogic.Borrowed;
import com.surelogic.Immutable;
import com.surelogic.RegionEffects;
import com.surelogic.Unique;

public class TestParameter {
	/* Driver for the U+F analysis needs to be updated.  Need @Borrowed parameter
	 * here to force the analysis to check this method.
	 */
	@RegionEffects("none")
	public void readParameter(final @Immutable int[] p, final @Borrowed Object o) {
		Object x = p;
	}
	
	/* Driver for the U+F analysis needs to be updated.  Need @Borrowed parameter
	 * here to force the analysis to check this method.
	 */
	@RegionEffects("none")
	public void writeParameter(@Immutable int[] p, final @Unique int[] u, final @Borrowed Object o) {
		p = u;
	}
	
	/* Driver for the U+F analysis needs to be updated.  Need @Borrowed parameter
	 * here to force the analysis to check this method.
	 */
	@RegionEffects("none")
	public void testParameterRead2(final @Immutable int[] p, final @Borrowed Object o) {
		// Read effect on immutable should be discarded
		int z = p[0];
	}
	
	/* Driver for the U+F analysis needs to be updated.  Need @Borrowed parameter
	 * here to force the analysis to check this method.
	 */
	@RegionEffects("writes any(java.lang.Object):Instance")
	public void testParameterWrite2(final @Immutable int[] p, final int v, final @Borrowed Object o) {
		// CHECKED BY FLOW ANALYSIS: Write effect on immutable is illegal
		p[0] = v;
	}
}
