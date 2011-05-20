package test;

import com.surelogic.Containable;
import com.surelogic.Unique;

@Containable(verify=false)
public enum TestContainableNoVerify {
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
	private TestContainableNoVerify(int a, int b, int c) {
	  this.x = a;
	  this.y = new Other(b, c);
  }
}
