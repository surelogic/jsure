package testBinder.staticImportFromSuper;

import static testBinder.unboxingToMatchGeneric.ExtendsStatic.foo;
import static testBinder.unboxingToMatchGeneric.ExtendsStatic.*;

import com.surelogic.*;

@Region("Foo")
public class Test2 {
	@Assume("@ThreadSafe for List")
	static void main() {
		foo("", true);
		
		new Nested();
	}

	@UniqueInRegion("Foo")
	Object bar;
	
	@UniqueInRegion("Instance into Instance")
	Object baz;
}
