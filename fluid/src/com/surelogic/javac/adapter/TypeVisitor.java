package com.surelogic.javac.adapter;

import java.util.*;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.signature.SignatureVisitor;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.operator.*;

public class TypeVisitor extends SignatureVisitor {	
	protected static final IRNode[] noNodes = JavaGlobals.noNodes;
	protected static final IRNode illegal     = new MarkedIRNode("illegal");
	
	private IRNode type = illegal;
	List<IRNode> typeActuals = new ArrayList<IRNode>();
	private final boolean varargs;
		
	TypeVisitor() {
		this(false);
	}
	TypeVisitor(boolean varargs) {
		super(Opcodes.ASM4);
		this.varargs = varargs;
	}
	
    /////////////////////////////////
	// For a type formal:
	public void visitFormalTypeParameter(String name) {
		throw new UnsupportedOperationException();
	}	
	// Zero or one time
	public SignatureVisitor visitClassBound() {
		throw new UnsupportedOperationException();
	}
	// One or more times
	public SignatureVisitor visitInterfaceBound() {
		throw new UnsupportedOperationException();
	}

	/////////////////////////////////
	// For a type decl
	public SignatureVisitor visitSuperclass() {
		throw new UnsupportedOperationException();
	}	
	// One or more times
	public SignatureVisitor visitInterface() {
		throw new UnsupportedOperationException();
	}	
	
    /////////////////////////////////
	// For a method
	public SignatureVisitor visitParameterType() {
		throw new UnsupportedOperationException();
	}
	public SignatureVisitor visitReturnType() {
		throw new UnsupportedOperationException();
	}
	public SignatureVisitor visitExceptionType() {
		throw new UnsupportedOperationException();
	}
	
	private void checkForNullType() {
		if (type == illegal) {
			System.out.println("null type");
		}
	}
	
    /////////////////////////////////
	// For a type
	public void visitBaseType(char t) {
		switch (t) {
		case 'V':
			type = VoidType.prototype.jjtCreate();
			break;
		case 'Z':
			type = BooleanType.prototype.jjtCreate();
			break;
		case 'B':			
			type = ByteType.prototype.jjtCreate();
			break;
		case 'C':
			type = CharType.prototype.jjtCreate();
			break;
		case 'S':
			type = ShortType.prototype.jjtCreate();
			break;
		case 'I':
			type = IntType.prototype.jjtCreate();
			break;
		case 'J':
			type = LongType.prototype.jjtCreate();
			break;
		case 'F':
			type = FloatType.prototype.jjtCreate();
			break;
		case 'D':
			type = DoubleType.prototype.jjtCreate();
			break;
		default:
			throw new IllegalArgumentException();	
		}
		visitEnd();
	}

	public SignatureVisitor visitArrayType() {
		final TypeVisitor outer = this;
		return new TypeVisitor() {
			protected void finish() {
				if (varargs) {
					outer.type = VarArgsType.createNode(this.getType()); 
				} else {
					outer.type = ArrayType.createNode(this.getType(), 1); 
				}
				outer.visitEnd();				
			}
		};
	}

	public void visitTypeVariable(String name) {
		type = NamedType.createNode(name);
		visitEnd();
	}
	
	public void visitClassType(String name) {
		type = ClassAdapter.adaptTypeName(name);
		typeActuals.clear();
	}

	public void visitTypeArgument() {
		typeActuals.add(WildcardType.prototype.jjtCreate());
	}
	public SignatureVisitor visitTypeArgument(char boundType) {
		final TypeVisitor outer = this;
		return new TypeVisitor() {
			protected void finish() {
				IRNode t = this.getType();
				outer.typeActuals.add(t); // FIX what about the boundType?
			}
		};
	}
	
	private void makeParamType() {
		if (!typeActuals.isEmpty()) {
			IRNode actuals = TypeActuals.createNode(typeActuals.toArray(noNodes));
			type = ParameterizedType.createNode(type, actuals);
			typeActuals.clear();		
		}
	}
	
	public void visitInnerClassType(String name) {
		makeParamType();
		type = TypeRef.createNode(type, name);			
	}
	
	public final void visitEnd() {
		makeParamType();
		checkForNullType();
		finish();
	}
	
	protected void finish() {
		// Nothing to do
	}
	
	public IRNode getType() {
		return type;
	}
}
