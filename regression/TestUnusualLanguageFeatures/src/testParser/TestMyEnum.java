package testParser;

import com.surelogic.*;

public enum TestMyEnum {
	A, B, C;

	@Borrowed("this")
	TestMyEnum() {
		
	}
}
