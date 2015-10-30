package edu.cmu.cs.fluid.java.bind;

import java.io.IOException;
import java.io.PrintStream;

import com.surelogic.ast.IType;
import com.surelogic.ast.java.operator.IDeclarationNode;
import com.surelogic.common.util.*;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.IROutput;

/**
 * Used to represent infinite types, 
 * so some methods are modified to prevent infinite loops
 * 
 * @author Edwin
 */
public class JavaRefTypeProxy extends JavaReferenceType implements IJavaReferenceType {
	private final IJavaReferenceType first, second;
	private IJavaReferenceType type;
	private Boolean complete;
	private String unparse;
	
	JavaRefTypeProxy(IJavaReferenceType u, IJavaReferenceType v) {
		first = u;
		second = v;
	}
	
	IJavaReferenceType first() {
		return first;
	}
	
	IJavaReferenceType second() {
		return second;
	}
	
	Boolean isComplete() {
		return complete;
	}
	
	void start(String unparse) {
		if (complete != null) {
			throw new IllegalStateException();
		}
		complete = Boolean.FALSE;
		
		if (unparse == null) {
			throw new IllegalStateException();
		}
		this.unparse = unparse;
	}
	
	void finishType(IJavaReferenceType t) {
		if (type != null || t == null) {
			throw new IllegalStateException();
		}
		if (complete != Boolean.FALSE) {
			throw new IllegalStateException();
		}		
		type = t;	
		complete = Boolean.TRUE;
	}
	
	public IJavaReferenceType getResult() {
		if (complete != Boolean.TRUE) {
			throw new IllegalStateException();
		}
		return type;
	}
	
	@Override
	public String toString() {
		return unparse;
	}

	@Override
  public String toSourceText() {
		return type.toSourceText();
	}
	
	@Override
	public IJavaType getSuperclass(ITypeEnvironment env) {
		return type.getSuperclass(env);
	}

	@Override
	public Iteratable<IJavaType> getSupertypes(ITypeEnvironment env) {
		return type.getSupertypes(env);
	}

	@Override
	public boolean isEqualTo(ITypeEnvironment env, IJavaType t2) {
		return type.isEqualTo(env, t2);
	}
	
	@Override
	public boolean isAssignmentCompatible(ITypeEnvironment env, IJavaType t2,
			IRNode e2) {
		return type.isAssignmentCompatible(env, t2, e2);
	}

	@Override
	public boolean isSubtype(ITypeEnvironment env, IJavaType t2) {
		System.out.println("Calling isSubtype("+this+", "+t2+")");
		if (this.toString().equals("lub(testJSure.ThreadSafePromiseDrop, testJSure.ImmutablePromiseDrop)")) {
			System.out.println("Foudn the one I want");
		}
		return type.isSubtype(env, t2);
	}

	@Override
	public boolean isValid() {
		// It's valid as long as the rest is valid
		return type != null;// && type.isValid();
	}

	@Override
	public void printStructure(PrintStream out, int indent) {
		// Nothing to do, since it's cyclic
	}

	@Override
	public IJavaType subst(IJavaTypeSubstitution s) {
		return this;
	}

	@Override
	public boolean isAssignmentCompatibleTo(IType t) {
		return type.isAssignmentCompatibleTo(t);
	}

	@Override
	public boolean isSubtypeOf(IType t) {
		return type.isSubtypeOf(t);
	}

	@Override
	public IDeclarationNode getNode() {
		return type.getNode();
	}

	@Override
	void writeValue(IROutput out) throws IOException {
		// TODO Auto-generated method stub
	}
	
	@Override
	public void visit(Visitor v) {
		// Nothing to do, since it's cyclic
	}
}
