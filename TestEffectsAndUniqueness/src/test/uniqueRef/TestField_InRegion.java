package test.uniqueRef;


import com.surelogic.Borrowed;
import com.surelogic.Region;
import com.surelogic.RegionEffects;
import com.surelogic.Regions;
import com.surelogic.UniqueInRegion;

@Regions({
	@Region("public A"),
	@Region("public B")
})
public class TestField_InRegion {
	@UniqueInRegion("Instance into Instance, V1 into A, V2 into B")
	private final Var f = null;
	

	
	/* Driver for the U+F analysis needs to be updated.  Need @Borrowed parameter
	 * here to force the analysis to check this method.
	 */
	@RegionEffects("reads A")
	public void testFieldRead1(final @Borrowed Object o) {
		this.f.get1();
	}
	
	/* Driver for the U+F analysis needs to be updated.  Need @Borrowed parameter
	 * here to force the analysis to check this method.
	 */
	@RegionEffects("writes A")
	public void testFieldWrite1(final int v, final @Borrowed Object o) {
		this.f.set1(v);
	}
	

	
	/* Driver for the U+F analysis needs to be updated.  Need @Borrowed parameter
	 * here to force the analysis to check this method.
	 */
	@RegionEffects("reads B")
	public void testFieldRead2(final @Borrowed Object o) {
		this.f.get2();
	}
	
	/* Driver for the U+F analysis needs to be updated.  Need @Borrowed parameter
	 * here to force the analysis to check this method.
	 */
	@RegionEffects("writes B")
	public void testFieldWrite2(final int v, final @Borrowed Object o) {
		this.f.set2(v);
	}
}
