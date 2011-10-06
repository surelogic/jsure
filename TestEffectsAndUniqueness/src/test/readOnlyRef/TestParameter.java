package test.readOnlyRef;


import com.surelogic.Borrowed;
import com.surelogic.ReadOnly;
import com.surelogic.RegionEffects;
import com.surelogic.Unique;

public class TestParameter {
	/* Driver for the U+F analysis needs to be updated.  Need @Borrowed parameter
	 * here to force the analysis to check this method.
	 */
	@RegionEffects("none")
	public void readParameter(final @ReadOnly Var p, final @Borrowed Object o) {
		Object x = p;
	}
	
	/* Driver for the U+F analysis needs to be updated.  Need @Borrowed parameter
	 * here to force the analysis to check this method.
	 */
	@RegionEffects("none")
	public void writeParameter(@ReadOnly Var p, final @Unique Var u, final @Borrowed Object o) {
		p = u;
	}
	
	/* Driver for the U+F analysis needs to be updated.  Need @Borrowed parameter
	 * here to force the analysis to check this method.
	 */
	@RegionEffects("none")
	public void testParameterRead1(final @ReadOnly Var p, final @Borrowed Object o) {
		// Read effect should expand to "reads Object:All"
		p.get();
	}
	
	/* Driver for the U+F analysis needs to be updated.  Need @Borrowed parameter
	 * here to force the analysis to check this method.
	 */
	@RegionEffects("none")
	public void testParameterRead2(final @ReadOnly Var p, final @Borrowed Object o) {
		// Read effect should expand to "reads Object:All"
		int z = p.val;
	}
	
	/* Driver for the U+F analysis needs to be updated.  Need @Borrowed parameter
	 * here to force the analysis to check this method.
	 */
	@RegionEffects("none")
	public void testParameterWrite1(final @ReadOnly Var p, final int v, final @Borrowed Object o) {
		// CHECKED BY FLOW ANALYSIS: Write effect on ReadOnly is illegal
		p.set(v);
	}
	
	/* Driver for the U+F analysis needs to be updated.  Need @Borrowed parameter
	 * here to force the analysis to check this method.
	 */
	@RegionEffects("none")
	public void testParameterWrite2(final @ReadOnly Var p, final int v, final @Borrowed Object o) {
		// CHECKED BY FLOW ANALYSIS: Write effect on ReadOnly is illegal
		p.val = v;
	}
}
