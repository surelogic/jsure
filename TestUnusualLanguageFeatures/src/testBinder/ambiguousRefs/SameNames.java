package testBinder.ambiguousRefs;

public class SameNames {
	enum Property {
		FOO, BAR, BAZ;
	}
	
	class Middle {
		class Inner extends Super {
			String foo() {
				return Property.FOO.toString();
			}
		}
	}
}

class Super {
	private enum Property {
		ALPHA, OMEGA
	}
}