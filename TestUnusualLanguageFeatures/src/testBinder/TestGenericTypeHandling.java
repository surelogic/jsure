package testBinder;

import java.util.*;

public class TestGenericTypeHandling {
	static class Inner {
		// Nothing to do
	}
	
	void takeArray(Inner[] a) {
		// Nothing to do
	}
	
	void testToArray() {
		List<Inner> inner = new ArrayList<Inner>();
		takeArray(inner.toArray(new Inner[inner.size()]));
	}
	
	/**
	 * Derived from:
	 * final IAASTRootNode aast = c.created.get(0);
			for(PromiseDrop<?> d : r.getStorage().getDrops(decl)) {
				if (useImplication ? d.getAST().implies(aast) : d.getAST().isSameAs(aast)) {
					return d;
				}
			}
	 */
	void testWildcardExtension() {
		Base base = null;
		Parameterized<?> callee = null;
		callee.getBase().implies(base);
	}
	
	static class Base {	
		boolean implies(Base b2) {
			return true;
		}
	}
	
	static class Parameterized<T extends Base> {		
		T getBase() {
			return null;
		}
	}
}
