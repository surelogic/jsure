package test.readOnlyRef.array.indirect;


import com.surelogic.Borrowed;
import com.surelogic.ReadOnly;
import com.surelogic.RegionEffects;

public class TestParameter {
	/* Driver for the U+F analysis needs to be updated.  Need @Borrowed parameter
	 * here to force the analysis to check this method.
	 */
	@RegionEffects("none")
	public void testParameterRead2(final @ReadOnly int[] p, final @Borrowed Object o) {
		// Read effect should expand to "reads Object:All"
		int[] a = p;
		int[] b = a;
		int z = b[0];
	}
	
	/* Driver for the U+F analysis needs to be updated.  Need @Borrowed parameter
	 * here to force the analysis to check this method.
	 */
	@RegionEffects("writes any(java.lang.Object):Instance")
	public void testParameterWrite(final @ReadOnly int[] p, final int v, final @Borrowed Object o) {
		// CHECKED BY FLOW ANALYSIS: Write effect on ReadOnly is illegal
		int[] a = p;
		int[] b = a;
		b[0] = v;
	}
}
