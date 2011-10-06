package test.immutableRef.indirect;

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
		Var a = x;
		Var b = a;
		readsVar(b);
	}
	
	@RegionEffects("none")
	public void caller2(final @Immutable Var y) {
		// No effects because y is Immutable ref!
		Var a = y;
		Var b = a;
		readsVar(b);
	}
}
