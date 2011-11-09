package test.immutableRef.array;

import com.surelogic.Borrowed;
import com.surelogic.Immutable;
import com.surelogic.RegionEffects;

public class TestParamPassing {
	@RegionEffects("reads v:Instance")
	public void readsVar(final @Borrowed int[] v) {
		int z = v[0];
	}
	
	@RegionEffects("none")
	public void caller1(final int[] x) {
		readsVar(x);
	}
	
	@RegionEffects("none")
	public void caller2(final @Immutable int[] y) {
		// No effects because y is Immutable ref!
		readsVar(y);
	}
}
