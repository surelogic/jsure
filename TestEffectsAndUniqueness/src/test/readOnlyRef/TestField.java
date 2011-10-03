package test.readOnlyRef;


import com.surelogic.Borrowed;
import com.surelogic.ReadOnly;
import com.surelogic.RegionEffects;
import com.surelogic.Unique;

public class TestField {
	@ReadOnly
	private Var f;
	
	
	/* Driver for the U+F analysis needs to be updated.  Need @Borrowed parameter
	 * here to force the analysis to check this method.
	 */
	@RegionEffects("reads Instance")
	public void readField(final @Borrowed Object o) {
		Object x = this.f;
	}
	
	/* Driver for the U+F analysis needs to be updated.  Need @Borrowed parameter
	 * here to force the analysis to check this method.
	 */
	@RegionEffects("writes Instance")
	public void writeField(final @Unique Var u, final @Borrowed Object o) {
		this.f = u;
	}
	
	/* Driver for the U+F analysis needs to be updated.  Need @Borrowed parameter
	 * here to force the analysis to check this method.
	 */
	@RegionEffects("reads Instance")
	public void testFieldRead(final @Borrowed Object o) {
		// TODO: Should be degraded to the effect "reads Object:All"
		this.f.get();
	}
	
	/* Driver for the U+F analysis needs to be updated.  Need @Borrowed parameter
	 * here to force the analysis to check this method.
	 */
	@RegionEffects("reads Instance; writes any(Var):Instance")
	public void testFieldWrite(final int v, final @Borrowed Object o) {
		// CHECKED BY FLOW ANALYSIS: Write effect on ReadOnly is illegal
		this.f.set(v);
	}
}
