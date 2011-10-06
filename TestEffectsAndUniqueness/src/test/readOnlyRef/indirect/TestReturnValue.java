package test.readOnlyRef.indirect;


import com.surelogic.Borrowed;
import com.surelogic.ReadOnly;
import com.surelogic.RegionEffects;

public class TestReturnValue {
	/* Driver for the U+F analysis needs to be updated.  Need @Borrowed parameter
	 * here to force the analysis to check this method.
	 */
	@RegionEffects("none")
	@ReadOnly("return")
	private Var methodCall(final @Borrowed Object o) {
		return new Var();
	}
	

	
	/* Driver for the U+F analysis needs to be updated.  Need @Borrowed parameter
	 * here to force the analysis to check this method.
	 */
	@RegionEffects("none")
	public void testReturnValueRead1(final @Borrowed Object o) {
		// Read effect on ReadOnly should be degraded to "reads Object:All"
		Var a = this.methodCall(null);
		Var b = a;
		b.get();
	}
	
	/* Driver for the U+F analysis needs to be updated.  Need @Borrowed parameter
	 * here to force the analysis to check this method.
	 */
	@RegionEffects("none")
	public void testReturnValueRead2(final @Borrowed Object o) {
		// Read effect on ReadOnly should be degraded to "reads Object:All"
		Var a = this.methodCall(null);
		Var b = a;
		int z = b.val;
	}
	
	/* Driver for the U+F analysis needs to be updated.  Need @Borrowed parameter
	 * here to force the analysis to check this method.
	 */
	@RegionEffects("none")
	public void testReturnValueWrite1(final int v, final @Borrowed Object o) {
		// CHECKED BY FLOW ANALYSIS: Write effect on immutable is illegal
		Var a = this.methodCall(null);
		Var b = a;
		b.set(v);
	}
	
	/* Driver for the U+F analysis needs to be updated.  Need @Borrowed parameter
	 * here to force the analysis to check this method.
	 */
	@RegionEffects("none")
	public void testReturnValueWrite2(final int v, final @Borrowed Object o) {
		// CHECKED BY FLOW ANALYSIS: Write effect on immutable is illegal
		Var a = this.methodCall(null);
		Var b = a;
		b.val = v;
	}
}
