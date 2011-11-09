package test.immutableRef.indirect;


import com.surelogic.Borrowed;
import com.surelogic.Immutable;
import com.surelogic.RegionEffects;

public class TestField {
	@Immutable
	private Var f;
	
	
	/* Driver for the U+F analysis needs to be updated.  Need @Borrowed parameter
	 * here to force the analysis to check this method.
	 */
	@RegionEffects("none")
	public void testFieldRead1(final @Borrowed Object o) {
		// Read effect on immutable should be discarded
		Var a = this.f;
		Var b = a;
		b.get();
	}
	
	/* Driver for the U+F analysis needs to be updated.  Need @Borrowed parameter
	 * here to force the analysis to check this method.
	 */
	@RegionEffects("none")
	public void testFieldRead2(final @Borrowed Object o) {
		// Read effect on immutable should be discarded
		Var a = this.f;
		Var b = a;
		int z = b.val;
	}
	
	/* Driver for the U+F analysis needs to be updated.  Need @Borrowed parameter
	 * here to force the analysis to check this method.
	 */
	@RegionEffects("none")
	public void testFieldWrite1(final int v, final @Borrowed Object o) {
		// CHECKED BY FLOW ANALYSIS: Write effect on immutable is illegal
		Var a = this.f;
		Var b = a;
		b.set(v);
	}
	
	/* Driver for the U+F analysis needs to be updated.  Need @Borrowed parameter
	 * here to force the analysis to check this method.
	 */
	@RegionEffects("none")
	public void testFieldWrite2(final int v, final @Borrowed Object o) {
		// CHECKED BY FLOW ANALYSIS: Write effect on immutable is illegal
		Var a = this.f;
		Var b = a;
		b.val = v;
	}
}
