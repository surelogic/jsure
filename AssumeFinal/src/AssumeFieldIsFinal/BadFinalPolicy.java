package AssumeFieldIsFinal;

import com.surelogic.Assume;
import com.surelogic.PolicyLock;

@PolicyLock("BadFinalLock is lock")
public class BadFinalPolicy {

	@Assume("Final")
	private Object lock;

	// Called only once at startup
	public void setLock(Object value) {
		lock = value;
	}

	public void tick() {
		synchronized (lock) {
			// do stuff
		}
	}
}
