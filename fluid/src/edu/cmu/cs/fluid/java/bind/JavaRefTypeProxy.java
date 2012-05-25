package edu.cmu.cs.fluid.java.bind;

import java.io.IOException;
import java.io.PrintStream;

import com.surelogic.ast.IType;
import com.surelogic.ast.java.operator.IDeclarationNode;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.IROutput;
import edu.cmu.cs.fluid.util.Iteratable;

/**
 * Used to represent infinite types, 
 * so some methods are modified to prevent infinite loops
 * 
 * @author Edwin
 */
public class JavaRefTypeProxy extends JavaReferenceType implements IJavaReferenceType {
	private IJavaReferenceType type;
	private Boolean complete;
	private String unparse;
	
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
	}
	
	@Override
	public String toString() {
		return unparse;
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
		return type.isSubtype(env, t2);
	}

	@Override
	public boolean isValid() {
		return type != null && type.isValid();
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
}
