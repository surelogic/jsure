package borrowedThis;

import com.surelogic.Borrowed;
import com.surelogic.Unique;

public class RelyOnBorrowedThis {
	@Unique
	private final Other other;
	
	public RelyOnBorrowedThis() {
		other = new Other();
	}
}

class Other {
	@Borrowed("this")
	public Other() {
		super();
	}
}

