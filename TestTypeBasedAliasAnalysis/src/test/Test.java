
package test;

import java.util.ArrayList;

public class Test {
	public void method(
			Object obj,
			ArrayList<String> stringList,
			ArrayList<Integer> intList,
			ArrayList<Object> objList,
			ArrayList<? extends Object> objList2) {
		// blank
	}
	
	public void method2(
			A a,
			B b,
			C c,
			D d,
			I i,
			J j,
			II ii,
			JJ jj
			) {
		// blank
	}
}

class A {}
class B {}
class C extends A {}
class D extends B {}

interface I {}
interface J {}
interface II extends I {}
interface JJ extends J {}
