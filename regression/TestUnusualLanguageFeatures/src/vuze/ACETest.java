package vuze;

public class ACETest {
	class Inner {
		Object innerField;
	}
	
	Object foo() {
		return new Inner() {
			@Override
			public String toString() {
				return new Object() {
					@Override
					public int hashCode() {
						//return innerField.hashCode();
						return 0;
					}
					@Override
					public boolean equals(Object o) {
						return false;
					}
				}.toString();
			}
		};
	}
}
