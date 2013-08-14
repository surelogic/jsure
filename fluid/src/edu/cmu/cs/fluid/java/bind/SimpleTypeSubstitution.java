package edu.cmu.cs.fluid.java.bind;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SimpleTypeSubstitution extends AbstractTypeSubstitution {

	private final List<IJavaTypeFormal> formals;
	private final List<IJavaType> actuals;
	
	public SimpleTypeSubstitution(IBinder b, List<IJavaTypeFormal> formals, List<? extends IJavaType> actuals) {
		super(b);
		this.formals = formals;
		this.actuals = new ArrayList<IJavaType>();
		int n = formals.size();
		Iterator<? extends IJavaType> it = actuals.iterator();
		for (int i=0; i < n; ++i) {
			IJavaTypeFormal jtf = formals.get(i);
			this.actuals.add(super.captureWildcardType(jtf, jtf.getDeclaration(), it.next()));
		}
	}
	
	@Override
	public IJavaType get(IJavaTypeFormal jtf) {
		int n = formals.size();
		for (int i=0; i < n; ++i) {
			if (formals.get(i) == jtf) return actuals.get(i);
		}
		return jtf;
	}
	
	// WARNING: we don't use "process" at all
	@Override
	protected <V> V process(IJavaTypeFormal jtf, Process<V> processor) {
		return null;
	}

}
