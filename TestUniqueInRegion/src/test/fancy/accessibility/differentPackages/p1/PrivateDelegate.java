package test.fancy.accessibility.differentPackages.p1;

import com.surelogic.Borrowed;
import com.surelogic.Region;

@Region("private PrivateAgg")
public class PrivateDelegate {
	@Borrowed("this")
	public PrivateDelegate() {
		super();
	}
}
