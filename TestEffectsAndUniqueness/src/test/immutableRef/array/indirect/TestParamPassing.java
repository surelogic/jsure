package test.immutableRef.array.indirect;

import com.surelogic.Borrowed;
import com.surelogic.Immutable;
import com.surelogic.RegionEffects;

public class TestParamPassing {
	@RegionEffects("reads v:Instance")
	public void readsVar(final @Borrowed int[] v) {
		int z = v[0];
	}
	
	@RegionEffects("reads x:Instance")
	public void caller1(final int[] x) {
		int[] a = x;
		int[] b = a;
		readsVar(b);
	}
	
	@RegionEffects("reads nothing")
	public void caller2(final @Immutable int[] y) {
		// No effects because y is Immutable ref!
		int[] a = y;
		int[] b = a;
		readsVar(b);
	}
}
