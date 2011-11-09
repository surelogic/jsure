package vuze;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

public class AccessTest {
	private final Map commands = new LinkedHashMap();
	
	void foo() {
		new Parent() {
			void bar() {
				commands.get(new Object());
			}
		};
	}
}

class Parent {
	private HashSet commands;
}