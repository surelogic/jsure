package test.borrowedField;

import com.surelogic.Borrowed;
import com.surelogic.InRegion;
import com.surelogic.Region;
import com.surelogic.RegionEffects;
import com.surelogic.Regions;
import com.surelogic.Unique;

@Regions({
	@Region("public V1"),
	@Region("public V2")
})
public class Var {
	@InRegion("V1")
	private int val1;
	
	@InRegion("V2")
	private int val2;
	
	
	
	@Unique("return")
	@RegionEffects("none")
	public Var() {
		super();
	}

	
	
	@RegionEffects("reads V1")
	@Borrowed("this")
	public int get1() {
		return val1;
	}
	
	@RegionEffects("writes V1")
	@Borrowed("this")
	public void set1(final int v) {
		val1 = v;
	}

	@RegionEffects("reads V2")
	@Borrowed("this")
	public int get2() {
		return val2;
	}
	
	@RegionEffects("writes V2")
	@Borrowed("this")
	public void set2(final int v) {
		val2 = v;
	}
}
