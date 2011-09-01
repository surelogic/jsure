package testBinder;

public class TestBindingOfNames {
	Object ambiguous;
	
	String ambiguous() {
		return null;
	}
	
	void foo() {
		ambiguous:
			// Make sure that we don't bind to the labelled statement
			System.out.println(ambiguous);
	}
}
