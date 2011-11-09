package test.borrowedReadOnlyField.indirect;


import com.surelogic.Borrowed;
import com.surelogic.ReadOnly;
import com.surelogic.RegionEffects;

public class TestField {
	@ReadOnly
	@Borrowed
	private final Var f = null;
	

	
	/* Driver for the U+F analysis needs to be updated.  Need @Borrowed parameter
	 * here to force the analysis to check this method.
	 */
	@RegionEffects("none")
	public void testFieldRead1(final @Borrowed Object o) {
		final Var a = this.f;
		final Var b = a;
		b.get1();
	}
	
	/* Driver for the U+F analysis needs to be updated.  Need @Borrowed parameter
	 * here to force the analysis to check this method.
	 */
	@RegionEffects("none")
	public void testFieldWrite1(final int v, final @Borrowed Object o) {
		// NOT ALLOWED
		final Var a = this.f;
		final Var b = a;
		b.set1(v);
	}
	

	
	/* Driver for the U+F analysis needs to be updated.  Need @Borrowed parameter
	 * here to force the analysis to check this method.
	 */
	@RegionEffects("none")
	public void testFieldRead2(final @Borrowed Object o) {
		final Var a = this.f;
		final Var b = a;
		b.get2();
	}
	
	/* Driver for the U+F analysis needs to be updated.  Need @Borrowed parameter
	 * here to force the analysis to check this method.
	 */
	@RegionEffects("none")
	public void testFieldWrite2(final int v, final @Borrowed Object o) {
		// NOT ALLOWED
		final Var a = this.f;
		final Var b = a;
		b.set2(v);
	}
}
