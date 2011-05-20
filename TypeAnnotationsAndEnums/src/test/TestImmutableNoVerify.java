package test;

import com.surelogic.Immutable;

@Immutable(verify=false)
public enum TestImmutableNoVerify {
	A(0, 0),
	
	B(1, 1),
	
	C(2, 2) {
		private long q;
	},
	
	D(3, 3) {
		private final Object s = new Object();
	},
	
	E(4, 4) {
		private final long t = 10;
	};
	
	
	
	private final int f;
	private final OtherImmutable o;
	
	private TestImmutableNoVerify(final int a, final int b) {
		f = a;
		o = new OtherImmutable(b);
	}
	
	public int getF() {
		return f;
	}
	
	public int getO() {
		return o.getV();
	}
}
