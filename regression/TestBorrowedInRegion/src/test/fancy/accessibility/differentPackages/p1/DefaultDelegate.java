package test.fancy.accessibility.differentPackages.p1;

import com.surelogic.Borrowed;
import com.surelogic.Region;

@Region("DefaultAgg")
public class DefaultDelegate {
	@Borrowed("this")
	public DefaultDelegate() {
		super();
	}
}
