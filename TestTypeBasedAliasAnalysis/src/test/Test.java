
package test;

import java.io.Serializable;
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
	
	public void arrays(
			Object o,
			Cloneable c,
			Serializable s,
			I i,
			A[] aArray,
			B[] bArray,
			C[] cArray,
			D[] dArray,
			I[] iArray,
			J[] jArray) {
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
