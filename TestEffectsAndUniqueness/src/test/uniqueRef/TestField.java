package test.uniqueRef;


import com.surelogic.Borrowed;
import com.surelogic.RegionEffects;
import com.surelogic.Unique;

public class TestField {
	@Unique
	private final Var f = null;
	

	
	/* Driver for the U+F analysis needs to be updated.  Need @Borrowed parameter
	 * here to force the analysis to check this method.
	 */
	@RegionEffects("reads Instance")
	public void testFieldRead1(final @Borrowed Object o) {
		this.f.get1();
	}
	
	/* Driver for the U+F analysis needs to be updated.  Need @Borrowed parameter
	 * here to force the analysis to check this method.
	 */
	@RegionEffects("writes Instance")
	public void testFieldWrite1(final int v, final @Borrowed Object o) {
		this.f.set1(v);
	}
	

	
	/* Driver for the U+F analysis needs to be updated.  Need @Borrowed parameter
	 * here to force the analysis to check this method.
	 */
	@RegionEffects("reads Instance")
	public void testFieldRead2(final @Borrowed Object o) {
		this.f.get2();
	}
	
	/* Driver for the U+F analysis needs to be updated.  Need @Borrowed parameter
	 * here to force the analysis to check this method.
	 */
	@RegionEffects("writes Instance")
	public void testFieldWrite2(final int v, final @Borrowed Object o) {
		this.f.set2(v);
	}
}
