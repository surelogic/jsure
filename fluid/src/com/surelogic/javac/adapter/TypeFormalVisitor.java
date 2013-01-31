package com.surelogic.javac.adapter;

import java.util.*;

import org.objectweb.asm.signature.SignatureVisitor;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.operator.*;

public class TypeFormalVisitor extends TypeVisitor {
	List<IRNode> formals = new ArrayList<IRNode>();
	String formalName;
	List<IRNode> bounds = new ArrayList<IRNode>();

	protected void buildFormal() {
		if (formalName == null) {
			return;
		}
		IRNode bounds = MoreBounds.createNode(this.bounds.toArray(noNodes));
		IRNode formal = TypeFormal.createNode(formalName, bounds);
		formals.add(formal);
	}
	
	@Override
	public void visitFormalTypeParameter(String name) {
		buildFormal();		
		formalName = name;
		bounds.clear();
	}	
	// Zero or one time
	@Override
	public SignatureVisitor visitClassBound() {
		return new TypeVisitor() {
			@Override
      protected void finish() {
				TypeFormalVisitor.this.bounds.add(this.getType());
			}
		};
	}
	// One or more times
	@Override
	public SignatureVisitor visitInterfaceBound() {
		return new TypeVisitor() {
			@Override
      protected void finish() {
				TypeFormalVisitor.this.bounds.add(this.getType());
			}
		};
	}
	
	// FIX how to get the result built?
}
