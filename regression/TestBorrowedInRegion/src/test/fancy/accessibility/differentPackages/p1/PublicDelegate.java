package test.fancy.accessibility.differentPackages.p1;

import com.surelogic.Borrowed;
import com.surelogic.Region;

@Region("public PublicAgg")
public class PublicDelegate {
	@Borrowed("this")
	public PublicDelegate() {
		super();
	}
}
