package intrinsicLock.qualifiedSuper;

import com.surelogic.Unique;

public class Outer {
	public class Inner {
		@Unique("return")
		public Inner() {
			super();
		}
	}
}
