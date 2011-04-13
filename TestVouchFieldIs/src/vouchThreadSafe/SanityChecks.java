package vouchThreadSafe;

import com.surelogic.Vouch;

@SuppressWarnings("unused")
public class SanityChecks {
	// GOOD: class not explicitly thread safe
  @Vouch("ThreadSafe")
	private UnannotatedClass good1;
	
	// GOOD: class explicitly thread safe
	@Vouch("ThreadSafe")
	private ThreadSafeClass good2;
	
	// GOOD: Array type
	@Vouch("ThreadSafe")
	private int[] good3;
	
	// BAD: class explicitly not thread safe
	@Vouch("ThreadSafe")
	private NotThreadSafeClass bad1;
	
	// BAD: primitively typed
	@Vouch("ThreadSafe")
	private int bad2;
}
