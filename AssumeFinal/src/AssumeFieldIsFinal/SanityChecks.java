package AssumeFieldIsFinal;

import com.surelogic.Assume;

@SuppressWarnings("unused")
public class SanityChecks {
  @Assume("final" /* is CONSISTENT */)
	private int assumedFinal;
	
	@Assume("Final" /* is UNASSOCIATED */) 
	private final int alreadyFinal = 1;
}
