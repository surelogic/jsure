package vouchFinal;

import com.surelogic.Vouch;

@SuppressWarnings("unused")
public class SanityChecks {
  @Vouch("final" /* is CONSISTENT */)
	private int assumedFinal;
	
	@Vouch("Final" /* is UNASSOCIATED */) 
	private final int alreadyFinal = 1;
}
