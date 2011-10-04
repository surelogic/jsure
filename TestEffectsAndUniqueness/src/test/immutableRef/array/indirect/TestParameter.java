package test.immutableRef.array.indirect;


import com.surelogic.Borrowed;
import com.surelogic.Immutable;
import com.surelogic.RegionEffects;

public class TestParameter {
	/* Driver for the U+F analysis needs to be updated.  Need @Borrowed parameter
	 * here to force the analysis to check this method.
	 */
	@RegionEffects("none")
	public void testParameterRead2(final @Immutable int[] p, final @Borrowed Object o) {
		// Read effect on immutable should be discarded
		int[] a = p;
		int[] b = a;
		int z = b[0];
	}
	
	/* Driver for the U+F analysis needs to be updated.  Need @Borrowed parameter
	 * here to force the analysis to check this method.
	 */
	@RegionEffects("writes any(java.lang.Object):Instance")
	public void testParameterWrite2(final @Immutable int[] p, final int v, final @Borrowed Object o) {
		// CHECKED BY FLOW ANALYSIS: Write effect on immutable is illegal
		int[] a = p;
		int[] b = a;
		b[0] = v;
	}
}
