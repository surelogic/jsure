package test.immutableRef.array;


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
		int z = this.methodCall(null)[0];
	}
	
	/* Driver for the U+F analysis needs to be updated.  Need @Borrowed parameter
	 * here to force the analysis to check this method.
	 */
	@RegionEffects("writes any(java.lang.Object):Instance")
	public void testReturnValueWrite2(final int v, final @Borrowed Object o) {
		// CHECKED BY FLOW ANALYSIS: Write effect on immutable is illegal
		this.methodCall(null)[0] = v;
	}
}
