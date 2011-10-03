package test.immutableRef;


import com.surelogic.Borrowed;
import com.surelogic.Immutable;
import com.surelogic.RegionEffects;
import com.surelogic.Unique;

public class TestParameter {
	/* Driver for the U+F analysis needs to be updated.  Need @Borrowed parameter
	 * here to force the analysis to check this method.
	 */
	@RegionEffects("none")
	public void readParameter(final @Immutable Var p, final @Borrowed Object o) {
		Object x = p;
	}
	
	/* Driver for the U+F analysis needs to be updated.  Need @Borrowed parameter
	 * here to force the analysis to check this method.
	 */
	@RegionEffects("none")
	public void writeParameter(@Immutable Var p, final @Unique Var u, final @Borrowed Object o) {
		p = u;
	}
	
	/* Driver for the U+F analysis needs to be updated.  Need @Borrowed parameter
	 * here to force the analysis to check this method.
	 */
	@RegionEffects("none")
	public void testParameterRead(final @Immutable Var p, final @Borrowed Object o) {
		// Read effect on immutable should be discarded
		p.get();
	}
	
	/* Driver for the U+F analysis needs to be updated.  Need @Borrowed parameter
	 * here to force the analysis to check this method.
	 */
	@RegionEffects("writes any(Var):Instance")
	public void testParameterWrite(final @Immutable Var p, final int v, final @Borrowed Object o) {
		// CHECKED BY FLOW ANALYSIS: Write effect on immutable is illegal
		p.set(v);
	}
}
