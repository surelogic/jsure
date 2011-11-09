package vouchFinal;

import com.surelogic.PolicyLock;
import com.surelogic.Vouch;

@PolicyLock("BadFinalLock is lock")
public class BadFinalPolicy {

	@Vouch("Final")
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
