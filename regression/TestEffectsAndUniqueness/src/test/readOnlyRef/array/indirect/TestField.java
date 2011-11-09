package test.readOnlyRef.array.indirect;


import com.surelogic.Borrowed;
import com.surelogic.ReadOnly;
import com.surelogic.RegionEffects;

public class TestField {
	@ReadOnly
	private int[] f;
	
	
	/* Driver for the U+F analysis needs to be updated.  Need @Borrowed parameter
	 * here to force the analysis to check this method.
	 */
	@RegionEffects("none")
	public void testFieldRead2(final @Borrowed Object o) {
		// TODO: Should be degraded to the effect "reads Object:All"
		int[] a = this.f;
		int[] b = a;
		int z = b[0];
	}
	
	/* Driver for the U+F analysis needs to be updated.  Need @Borrowed parameter
	 * here to force the analysis to check this method.
	 */
	@RegionEffects("none")
	public void testFieldWrite2(final int v, final @Borrowed Object o) {
		// CHECKED BY FLOW ANALYSIS: Write effect on ReadOnly is illegal
		int[] a = this.f;
		int[] b = a;
		b[0] = v;
	}
}
