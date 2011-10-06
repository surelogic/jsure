package test.immutableRef.array;


import com.surelogic.Borrowed;
import com.surelogic.Immutable;
import com.surelogic.RegionEffects;
import com.surelogic.Unique;

public class TestField {
	@Immutable
	private int[] f;
	
	
	/* Driver for the U+F analysis needs to be updated.  Need @Borrowed parameter
	 * here to force the analysis to check this method.
	 */
	@RegionEffects("none")
	public void readField(final @Borrowed Object o) {
		Object x = this.f;
	}
	
	/* Driver for the U+F analysis needs to be updated.  Need @Borrowed parameter
	 * here to force the analysis to check this method.
	 */
	@RegionEffects("none")
	public void writeField(final @Unique int[] u, final @Borrowed Object o) {
		this.f = u;
	}

	/* Driver for the U+F analysis needs to be updated.  Need @Borrowed parameter
	 * here to force the analysis to check this method.
	 */
	@RegionEffects("none")
	public void testFieldRead2(final @Borrowed Object o) {
		// Read effect on immutable should be discarded
		int z = this.f[0];
	}
	
	/* Driver for the U+F analysis needs to be updated.  Need @Borrowed parameter
	 * here to force the analysis to check this method.
	 */
	@RegionEffects("none")
	public void testFieldWrite2(final int v, final @Borrowed Object o) {
		// CHECKED BY FLOW ANALYSIS: Write effect on immutable is illegal
		this.f[0] = v;
	}
}
