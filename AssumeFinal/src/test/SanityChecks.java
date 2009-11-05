package test;

import com.surelogic.AssumeFinal;

public class SanityChecks {
	@AssumeFinal(/* is CONSISTENT */)
	private int assumedFinal;
	
	@AssumeFinal(/* is UNASSOCIATED */) 
	private final int alreadyFinal = 1;
	
	public void method(
			@AssumeFinal(/* is CONSISTENT */) Object assumedFinal,
			@AssumeFinal(/* is UNASSOCIATED */) final Object alreadyFinal) {
		// do nothing
	}
}
