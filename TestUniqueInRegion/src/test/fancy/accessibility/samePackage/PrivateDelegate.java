package test.fancy.accessibility.samePackage;

import com.surelogic.Borrowed;
import com.surelogic.Region;

@Region("private PrivateAgg")
public class PrivateDelegate {
	@Borrowed("this")
	public PrivateDelegate() {
		super();
	}
}
