package test;

import com.surelogic.Borrowed;
import com.surelogic.Unique;

public class TestUniqueReturn {
	@Borrowed("this") // good
	public TestUniqueReturn() {
		super();
	}
	
	@Unique("return") // good
	public TestUniqueReturn(int x) {
		super();
	}

	@Borrowed("return") // illegal
	public TestUniqueReturn(boolean flag) {
		super();
	}
	
	@Unique("this") // illegal
	public TestUniqueReturn(int x, boolean flag) {
		super();
	}

	@Borrowed("this")
	@Unique("return") // redundant
	public TestUniqueReturn(int x, int y) {
		super();
	}
}
