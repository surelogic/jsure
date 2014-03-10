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
	@Override
  public void visitFormalTypeParameter(String name) {
		throw new UnsupportedOperationException();
	}	
	// Zero or one time
	@Override
  public SignatureVisitor visitClassBound() {
		throw new UnsupportedOperationException();
	}
	// One or more times
	@Override
  public SignatureVisitor visitInterfaceBound() {
		throw new UnsupportedOperationException();
	}

	/////////////////////////////////
	// For a type decl
	@Override
  public SignatureVisitor visitSuperclass() {
		throw new UnsupportedOperationException();
	}	
	// One or more times
	@Override
  public SignatureVisitor visitInterface() {
		throw new UnsupportedOperationException();
	}	
	
    /////////////////////////////////
	// For a method
	@Override
  public SignatureVisitor visitParameterType() {
		throw new UnsupportedOperationException();
	}
	@Override
  public SignatureVisitor visitReturnType() {
		throw new UnsupportedOperationException();
	}
	@Override
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
	@Override
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

	@Override
  public SignatureVisitor visitArrayType() {
		final TypeVisitor outer = this;
		return new TypeVisitor() {
			@Override
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

	@Override
  public void visitTypeVariable(String name) {
		type = NamedType.createNode(name);
		visitEnd();
	}
	
	@Override
  public void visitClassType(String name) {
		type = ClassAdapter.adaptTypeName(name);
		typeActuals.clear();
	}

    /**
     * Visits an unbounded type argument of the last visited class or inner
     * class type.
     */
	@Override
  public void visitTypeArgument() {
		// TODO Should this be raw?
		typeActuals.add(WildcardType.prototype.jjtCreate());
	}

    /**
     * Visits a type argument of the last visited class or inner class type.
     * 
     * @param wildcard
     *            '+', '-' or '='.
     * @return a non null visitor to visit the signature of the type argument.
     */
	@Override
  public SignatureVisitor visitTypeArgument(final char wildcardType) {
		final TypeVisitor outer = this;
		return new TypeVisitor() {
			@Override
      protected void finish() {
				IRNode t = this.getType();
				switch (wildcardType) {
				case EXTENDS:
					t = WildcardExtendsType.createNode(t);
					break;
				case SUPER:
					t = WildcardSuperType.createNode(t);
					break;
				case INSTANCEOF:
//					System.out.println("What to do with "+DebugUnparser.toString(t));
				default:
				}
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
	
	@Override
  public void visitInnerClassType(String name) {
		makeParamType();
		type = TypeRef.createNode(type, name);			
	}
	
	@Override
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
