package edu.cmu.cs.fluid.java.bind;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.util.Iteratable;

/**
 * Meant to have some of the methods overridden
 * 
 * @author Edwin
 */
public abstract class BinderWrapper implements IBinder {
	protected final IBinder binder;
	
	public BinderWrapper(IBinder b) {
		binder = b;
	}
	
	public void disableWarnings() {
		binder.disableWarnings();
	}

	public void enableWarnings() {
		binder.enableWarnings();
	}

	public <T> T findClassBodyMembers(IRNode type,
			ISuperTypeSearchStrategy<T> tvs, boolean throwIfNotFound) {
		return binder.findClassBodyMembers(type, tvs, throwIfNotFound);
	}

	public Iteratable<IBinding> findOverriddenMethods(IRNode methodDeclaration) {
		return binder.findOverriddenMethods(methodDeclaration);
	}

	public Iteratable<IBinding> findOverriddenParentMethods(IRNode mth) {
		return binder.findOverriddenParentMethods(mth);
	}
	
	public Iteratable<IRNode> findOverridingMethodsFromType(IRNode callee,
			IRNode receiverType) {
		return binder.findOverridingMethodsFromType(callee, receiverType);
	}

	public IRNode getBinding(IRNode name) {
		return binder.getBinding(name);
	}

	public IBinding getIBinding(IRNode node) {
		return binder.getIBinding(node);
	}

	public IBinding getIBinding(IRNode node, IRNode context) {
		return binder.getIBinding(node, context);
	}
	
	public IJavaType getJavaType(IRNode n) {
		return binder.getJavaType(n);
	}

	@SuppressWarnings("deprecation")
	public IJavaDeclaredType getSuperclass(IJavaDeclaredType type) {
		return binder.getSuperclass(type);
	}

	public ITypeEnvironment getTypeEnvironment() {
		return binder.getTypeEnvironment();
	}
}
