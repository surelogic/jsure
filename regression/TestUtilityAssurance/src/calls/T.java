package calls;

import com.surelogic.Utility;

/* Tests that creation of instances of the class is detected, and that
 * no subclasses of the class are allowed.
 */
@Utility
@SuppressWarnings("unused")
public class T {
	// BAD: Creates an instance
	public static final Object f = new T();
	
	// BAD: Creates an instance
	public static final Object ff = new T() {};
	
	
	
	private T() {
		super();
	}
	
	
	
  private static T factory() {
		// BAD: Creates an instance of the utility class
		return new T();
	}
	
	
	private static Object factory2() {
		// BAD: Creates an instance of the utility class
		return new T() {
			public void foo() {}
		};
	}
	
	
	// BAD: Extends from the utility class
	public class U extends T {
		public U() {
			super();
		}

	
	
		// BAD: Extends from the utility class
		public class UU extends T {
			public UU() {
				super();
			}

		
		
			// BAD: Extends from the utility class
			public class UUU extends T {
				public UUU() {
					super();
				}
			}
		}
	}
	
	
	// GOOD
	public static class Q {
		// BAD: Creates an instance
		public static final Object f = new T();
		
		// BAD: Creates an instance
		public static final Object ff = new T() {};

		
		
		public Object m1() {
			// BAD: Creates an instance of the utility class
			return new T();
		}

		private static Object m2() {
			// BAD: Creates an instance of the utility class
			return new T() {
				public void foo() {}
			};
		}

	
	
		// GOOD
		public static class QQ {
			// BAD: Creates an instance
			public static final Object f = new T();
			
			// BAD: Creates an instance
			public static final Object ff = new T() {};

			
			
			public Object m1() {
				// BAD: Creates an instance of the utility class
				return new T();
			}

			private static Object m2() {
				// BAD: Creates an instance of the utility class
				return new T() {
					public void foo() {}
				};
			}

			
			
			// GOOD
			public static class QQQ {
				// BAD: Creates an instance
				public static final Object f = new T();
				
				// BAD: Creates an instance
				public static final Object ff = new T() {};

				
				
				public Object m1() {
					// BAD: Creates an instance of the utility class
					return new T();
				}

				private static Object m2() {
					// BAD: Creates an instance of the utility class
					return new T() {
						public void foo() {}
					};
				}
			}
		}
	}
}
