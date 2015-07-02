package edu.cmu.cs.fluid.java.bind;

import com.surelogic.common.util.*;

import edu.cmu.cs.fluid.ir.IRNode;

/**
 * Meant to have some of the methods overridden
 * 
 * @author Edwin
 */
public abstract class BinderWrapper implements IBinder {
	private final IBinder binder;
	
	public BinderWrapper(IBinder b) {
		binder = b;
	}
	
	@Override
  public void disableWarnings() {
		binder.disableWarnings();
	}

	@Override
  public void enableWarnings() {
		binder.enableWarnings();
	}

	@Override
  public <T> T findClassBodyMembers(IRNode type,
			ISuperTypeSearchStrategy<T> tvs, boolean throwIfNotFound) {
		return binder.findClassBodyMembers(type, tvs, throwIfNotFound);
	}

	@Override
  public Iteratable<IBinding> findOverriddenMethods(IRNode methodDeclaration) {
		return binder.findOverriddenMethods(methodDeclaration);
	}

	@Override
  public Iteratable<IBinding> findOverriddenParentMethods(IRNode mth) {
		return binder.findOverriddenParentMethods(mth);
	}
	
	@Override
  public Iteratable<IRNode> findOverridingMethodsFromType(IRNode callee,
			IRNode receiverType) {
		return binder.findOverridingMethodsFromType(callee, receiverType);
	}

	@Override
  public IRNode getBinding(IRNode name) {
		return binder.getBinding(name);
	}

	@Override
  public IBinding getIBinding(IRNode node) {
		return binder.getIBinding(node);
	}

	@Override
  public IBinding getIBinding(IRNode node, IRNode context) {
		return binder.getIBinding(node, context);
	}
	
	@Override
  public IJavaType getJavaType(IRNode n) {
		return binder.getJavaType(n);
	}
	
	@Override
  @SuppressWarnings("deprecation")
	public IJavaDeclaredType getSuperclass(IJavaDeclaredType type) {
		return binder.getSuperclass(type);
	}

	@Override
  public ITypeEnvironment getTypeEnvironment() {
		return binder.getTypeEnvironment();
	}
}
