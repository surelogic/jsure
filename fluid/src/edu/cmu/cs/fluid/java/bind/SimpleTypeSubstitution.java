package edu.cmu.cs.fluid.java.bind;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SimpleTypeSubstitution extends AbstractTypeSubstitution {

	private final List<IJavaTypeFormal> formals;
	private List<IJavaType> actuals;
	
	public SimpleTypeSubstitution(IBinder b, List<IJavaTypeFormal> formals, List<? extends IJavaType> actuals) {
		super(b);
		this.formals = formals;
		this.actuals = new ArrayList<IJavaType>();
		int n = formals.size();
		if (n > 0 && actuals.size() == 0) {
			// hack: we replace all formals with their upperbounds
			// where each variable is replaced with their erased upper bounds.
			for (IJavaTypeFormal ft : formals) {
				this.actuals.add(b.getTypeEnvironment().computeErasure(ft));
			}
			ArrayList<IJavaType> newActuals = new ArrayList<IJavaType>();
			for (IJavaTypeFormal ft : formals) {
				newActuals.add(ft.getExtendsBound(b.getTypeEnvironment()).subst(this));
			}
			this.actuals = newActuals;
		} else {
			Iterator<? extends IJavaType> it = actuals.iterator();
			for (int i=0; i < n; ++i) {
				IJavaTypeFormal jtf = formals.get(i);
				this.actuals.add(super.captureWildcardType(jtf, jtf.getDeclaration(), it.next()));
			}
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
