package test.uniqueWriteRef;


import com.surelogic.Borrowed;
import com.surelogic.Region;
import com.surelogic.RegionEffects;
import com.surelogic.UniqueInRegion;

@Region("public X")
public class TestField_SimpleInRegion {
	@UniqueInRegion(allowRead=true, value="X")
	private final Var f = null;
	

	
	/* Driver for the U+F analysis needs to be updated.  Need @Borrowed parameter
	 * here to force the analysis to check this method.
	 */
	@RegionEffects("reads X")
	public void testFieldRead1(final @Borrowed Object o) {
		this.f.get1();
	}
	
	/* Driver for the U+F analysis needs to be updated.  Need @Borrowed parameter
	 * here to force the analysis to check this method.
	 */
	@RegionEffects("writes X")
	public void testFieldWrite1(final int v, final @Borrowed Object o) {
		this.f.set1(v);
	}
	

	
	/* Driver for the U+F analysis needs to be updated.  Need @Borrowed parameter
	 * here to force the analysis to check this method.
	 */
	@RegionEffects("reads X")
	public void testFieldRead2(final @Borrowed Object o) {
		this.f.get2();
	}
	
	/* Driver for the U+F analysis needs to be updated.  Need @Borrowed parameter
	 * here to force the analysis to check this method.
	 */
	@RegionEffects("writes X")
	public void testFieldWrite2(final int v, final @Borrowed Object o) {
		this.f.set2(v);
	}
}
