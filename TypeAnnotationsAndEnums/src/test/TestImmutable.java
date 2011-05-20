package test;

import com.surelogic.Immutable;

@Immutable
public enum TestImmutable {
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
	
	private TestImmutable(final int a, final int b) {
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


@Immutable
class OtherImmutable {
	private final int v;
	
	public OtherImmutable(final int x) {
		this.v = x;
	}
	
	public int getV() {
		return v;
	}
}