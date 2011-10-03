package test.readOnlyRef;


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
	public void testReturnValueRead(final @Borrowed Object o) {
		// Read effect on ReadOnly should be degraded to "reads Object:All"
		this.methodCall(null).get();
	}
	
	/* Driver for the U+F analysis needs to be updated.  Need @Borrowed parameter
	 * here to force the analysis to check this method.
	 */
	@RegionEffects("writes any(Var):Instance")
	public void testReturnValueWrite(final int v, final @Borrowed Object o) {
		// CHECKED BY FLOW ANALYSIS: Write effect on immutable is illegal
		this.methodCall(null).set(v);
	}
}
