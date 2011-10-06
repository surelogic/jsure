package test.immutableRef.array.indirect;


import com.surelogic.Borrowed;
import com.surelogic.Immutable;
import com.surelogic.RegionEffects;

public class TestReturnValue {
	/* Driver for the U+F analysis needs to be updated.  Need @Borrowed parameter
	 * here to force the analysis to check this method.
	 */
	@RegionEffects("none")
	@Immutable("return")
	private int[] methodCall(final @Borrowed Object o) {
		return new int[10];
	}
	

	
	/* Driver for the U+F analysis needs to be updated.  Need @Borrowed parameter
	 * here to force the analysis to check this method.
	 */
	@RegionEffects("none")
	public void testReturnValueRead2(final @Borrowed Object o) {
		// Read effect on immutable should be discarded
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
