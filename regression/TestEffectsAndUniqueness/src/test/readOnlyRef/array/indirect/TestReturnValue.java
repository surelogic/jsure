package test.readOnlyRef.array.indirect;


import com.surelogic.Borrowed;
import com.surelogic.ReadOnly;
import com.surelogic.RegionEffects;

public class TestReturnValue {
	/* Driver for the U+F analysis needs to be updated.  Need @Borrowed parameter
	 * here to force the analysis to check this method.
	 */
	@RegionEffects("none")
	@ReadOnly("return")
	private int[] methodCall(final @Borrowed Object o) {
		return new int[10];
	}
	

	
	/* Driver for the U+F analysis needs to be updated.  Need @Borrowed parameter
	 * here to force the analysis to check this method.
	 */
	@RegionEffects("none")
	public void testReturnValueRead2(final @Borrowed Object o) {
		// Read effect on ReadOnly should be degraded to "reads Object:All"
		int[] a = this.methodCall(null);
		int[] b = a;
		int z = a[0];
	}
	
	/* Driver for the U+F analysis needs to be updated.  Need @Borrowed parameter
	 * here to force the analysis to check this method.
	 */
	@RegionEffects("none")
	public void testReturnValueWrite2(final int v, final @Borrowed Object o) {
		// CHECKED BY FLOW ANALYSIS: Write effect on immutable is illegal
		int[] a = this.methodCall(null);
		int[] b = a;
		b[0] = v;
	}
}
