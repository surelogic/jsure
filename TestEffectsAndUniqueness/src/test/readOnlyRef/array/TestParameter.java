package test.readOnlyRef.array;


import com.surelogic.Borrowed;
import com.surelogic.ReadOnly;
import com.surelogic.RegionEffects;
import com.surelogic.Unique;

public class TestParameter {
	/* Driver for the U+F analysis needs to be updated.  Need @Borrowed parameter
	 * here to force the analysis to check this method.
	 */
	@RegionEffects("none")
	public void readParameter(final @ReadOnly int[] p, final @Borrowed Object o) {
		Object x = p;
	}
	
	/* Driver for the U+F analysis needs to be updated.  Need @Borrowed parameter
	 * here to force the analysis to check this method.
	 */
	@RegionEffects("none")
	public void writeParameter(@ReadOnly int[] p, final @Unique int[] u, final @Borrowed Object o) {
		p = u;
	}
	
	/* Driver for the U+F analysis needs to be updated.  Need @Borrowed parameter
	 * here to force the analysis to check this method.
	 */
	@RegionEffects("none")
	public void testParameterRead2(final @ReadOnly int[] p, final @Borrowed Object o) {
		// Read effect should expand to "reads Object:All"
		int z = p[0];
	}
	
	/* Driver for the U+F analysis needs to be updated.  Need @Borrowed parameter
	 * here to force the analysis to check this method.
	 */
	@RegionEffects("none")
	public void testParameterWrite(final @ReadOnly int[] p, final int v, final @Borrowed Object o) {
		// CHECKED BY FLOW ANALYSIS: Write effect on ReadOnly is illegal
		p[0] = v;
	}
}
