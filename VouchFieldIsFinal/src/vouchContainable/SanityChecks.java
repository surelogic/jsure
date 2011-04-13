package vouchContainable;

import com.surelogic.Vouch;

@SuppressWarnings("unused")
public class SanityChecks {
	// GOOD: class not explicitly containable
  @Vouch("Containable")
	private UnannotatedClass good1;
	
	// GOOD: class explicitly containable
	@Vouch("Containable")
	private ContainableClass good2;
	
	// GOOD: Array type
	@Vouch("Containable")
	private int[] good3;
	
	// BAD: class explicitly not containable
	@Vouch("Containable")
	private NotContainableClass bad1;
	
	// BAD: primitively typed
	@Vouch("Containable")
	private int bad2;
}
