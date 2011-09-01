package testBinder.strangeAnnos;

import java.lang.annotation.Annotation;

public class Impl {
	final int implCount;
	
	Impl() {
		implCount = 0;
	}
	
	class Nested implements Anno {
		public int implCount() {
			return implCount;
		}

		public Class<? extends Annotation> annotationType() {
			return Anno.class;
		}
	}
	
	int call(Anno a) {
		return a.implCount();
	}
	
	int call2(Nested a) {
		return a.implCount();
	}
	
	interface AnnoExt extends Anno {
		String unparse();
	}
}
