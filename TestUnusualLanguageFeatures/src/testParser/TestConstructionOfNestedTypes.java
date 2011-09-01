package testParser;

public class TestConstructionOfNestedTypes {
	class Inner {
		class Innermost {			
		}
	}
	Inner i = new Inner();
	
	Object foo() {
		//Inner i = new Inner();
		return i.new Innermost();
	}
}
