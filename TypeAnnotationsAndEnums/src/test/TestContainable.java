package test;

import com.surelogic.Containable;
import com.surelogic.Unique;

@Containable
public enum TestContainable {
	A(0, 1, 2) {
		int q;		
	},
	B(1, 2, 3) {
		NotContainable q;
	},
	C(2, 3, 4);
	
	private int x;
	@Unique
	private Other y;
	
	@Unique("return")
	private TestContainable(int a, int b, int c) {
	  this.x = a;
	  this.y = new Other(b, c);
  }
}

@Containable
class Other {
	private int a;
	private int b;
	
	@Unique("return")
	public Other(int a, int b) {
		this.a = a;
		this.b = b;
	}
}

class NotContainable {
}
