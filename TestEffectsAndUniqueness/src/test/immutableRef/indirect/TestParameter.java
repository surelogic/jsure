package test.immutableRef.indirect;


import com.surelogic.Borrowed;
import com.surelogic.Immutable;
import com.surelogic.RegionEffects;

public class TestParameter {
	/* Driver for the U+F analysis needs to be updated.  Need @Borrowed parameter
	 * here to force the analysis to check this method.
	 */
	@RegionEffects("none")
	public void testParameterRead1(final @Immutable Var p, final @Borrowed Object o) {
		// Read effect on immutable should be discarded
		Var a = p;
		Var b = a;
		b.get();
	}
	
	/* Driver for the U+F analysis needs to be updated.  Need @Borrowed parameter
	 * here to force the analysis to check this method.
	 */
	@RegionEffects("none")
	public void testParameterRead2(final @Immutable Var p, final @Borrowed Object o) {
		// Read effect on immutable should be discarded
		Var a = p;
		Var b = a;
		int z = b.val;
	}
	
	/* Driver for the U+F analysis needs to be updated.  Need @Borrowed parameter
	 * here to force the analysis to check this method.
	 */
	@RegionEffects("none")
	public void testParameterWrite1(final @Immutable Var p, final int v, final @Borrowed Object o) {
		// CHECKED BY FLOW ANALYSIS: Write effect on immutable is illegal
		Var a = p;
		Var b = a;
		b.set(v);
	}
	
	/* Driver for the U+F analysis needs to be updated.  Need @Borrowed parameter
	 * here to force the analysis to check this method.
	 */
	@RegionEffects("none")
	public void testParameterWrite2(final @Immutable Var p, final int v, final @Borrowed Object o) {
		// CHECKED BY FLOW ANALYSIS: Write effect on immutable is illegal
		Var a = p;
		Var b = a;
		b.val = v;
	}
}
