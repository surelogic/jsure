package testBinder.enums;

import com.surelogic.Immutable;

@Immutable
public enum F {
	A(0) {
		// BAD: mutable field
		private int val = 0;
		
		public void magic() {
			val += 1;
		}
	},
	
	X,
	
	Y(10),
	
	B(1) {
		// GOOD
		public void magic() {
			// nothing
		}
	};
	
	
	private final int id;
	
	private F() {
		this(-1);
	}
	
	private F(final int id) {
		this.id = id;
	}
	
	
	public int getId() {
		return id;
	}
	
	public void magic() {};
}
