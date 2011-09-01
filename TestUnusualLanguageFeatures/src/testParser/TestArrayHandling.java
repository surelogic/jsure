package testParser;

import com.surelogic.*;

public class TestArrayHandling {
	@UniqueInRegion("a")
	// Note that we're using two different array notations
	private int[] a[] = new int[10][2];
	private int[] b = new int[1], c[] = new int[10][2];

	@RegionEffects("none")
	public void doStuff() {		
		a
		[0][0] = 10;
		b[0] = 20;
		c[0] = a[0];
	}
}
