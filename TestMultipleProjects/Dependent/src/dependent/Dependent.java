package dependent;

import common.Thing;

import root.Root;

public class Dependent extends Root {
	@Override
	public String toString() {
		return print(thing);
	}
	
	static String print(Thing t) {
		return t.toString();
	}
}
