package vouchImmutable;

import com.surelogic.Vouch;

@SuppressWarnings("unused")
public class SanityChecks {
	// GOOD: class not explicitly immutable
  @Vouch("Immutable")
	private UnannotatedClass good1;
	
	// GOOD: class explicitly immutable
	@Vouch("Immutable")
	private ImmutableClass good2;
	
	// GOOD: Array type
	@Vouch("Immutable")
	private int[] good3;
	
	// BAD: class explicitly mutable
	@Vouch("Immutable")
	private MutableClass bad1;
	
	// BAD: primitively typed
	@Vouch("Immutable")
	private int bad2;
}
