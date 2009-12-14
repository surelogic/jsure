package borrowedThis;

import com.surelogic.Borrowed;

public class UseIt {
	private static UseIt other = null;
	
	@Borrowed("this")
	public UseIt() {
		other = null;
	}

	
	@Borrowed("this")
	public UseIt(final int bad) {
		other = this;
	}
}
