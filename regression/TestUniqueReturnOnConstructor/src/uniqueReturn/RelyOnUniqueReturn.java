package uniqueReturn;

//import com.surelogic.Borrowed;
import com.surelogic.Unique;

public class RelyOnUniqueReturn {
	@Unique
	private final Other other;
	
	public RelyOnUniqueReturn() {
		other = new Other();
	}
}

class Other {
	@Unique("return")
	public Other() {
		super();
	}
}
