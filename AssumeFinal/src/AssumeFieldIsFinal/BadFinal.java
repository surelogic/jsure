package AssumeFieldIsFinal;

import com.surelogic.Assume;
import com.surelogic.InRegion;
import com.surelogic.Region;
import com.surelogic.RegionLock;

@Region("private BadFinalRegion")
@RegionLock("BadFinalLock is lock protects BadFinalRegion")
public class BadFinal {

	@Assume("final")
	private Object lock;

	// Called only once at startup
	public void setLock(Object value) {
		lock = value;
	}

	@InRegion("BadFinalRegion")
	private int x, y;

	public void tick() {
		synchronized (lock) {
			x++;
			y++;
		}
	}
}
