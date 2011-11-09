package uniqueReturn;

import com.surelogic.Unique;

public class UseIt {
	private static UseIt other = null;
	
	@Unique("return")
	public UseIt() {
		other = null;
	}

	
	@Unique("return")
	public UseIt(final int bad) {
		other = this;
	}
}
