package test.immutableRef;

import com.surelogic.Borrowed;
import com.surelogic.Immutable;
import com.surelogic.RegionEffects;

public class TestParamPassing {
	@RegionEffects("reads v:Instance")
	public void readsVar(final @Borrowed Var v) {
		v.get();
	}
	
	@RegionEffects("none")
	public void caller1(final Var x) {
		readsVar(x);
	}
	
	@RegionEffects("none")
	public void caller2(final @Immutable Var y) {
		// No effects because y is Immutable ref!
		readsVar(y);
	}
}
