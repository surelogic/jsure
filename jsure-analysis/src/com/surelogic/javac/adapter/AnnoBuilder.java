package com.surelogic.javac.adapter;

import java.lang.reflect.Array;
import java.util.*;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.operator.*;

/**
 * Builds IR equivalent to the original annotation
 * 
 * @author Edwin
 */
// (visit | visitEnum | visitAnnotation | visitArray)* visitEnd. 
class AnnoBuilder extends AnnotationVisitor {
	final String type;
	final List<IRNode> pairs = new ArrayList<IRNode>();
	IRNode result;
	
	AnnoBuilder(String desc) {
		super(Opcodes.ASM5);
		if (desc == null) {
			type = null;
		} else {
			if (!desc.startsWith("L")) {
				throw new IllegalStateException("Unexpected type descriptor: "+desc);
			}
			final int semi = desc.indexOf(';');
			type = desc.substring(1, semi).replace('/', '.').replace('$', '.');
			// TODO this isn't quite right for nested annotation decls
			//System.out.println("Starting anno: "+type);
		}
	}
	
	static IRNode adaptValue(Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof String) {
			if (SourceAdapter.includeQuotesInStringLiteral) {
				return StringLiteral.createNode('"'+value.toString()+'"');
			}
			return StringLiteral.createNode(value.toString());
		}
		else if (value instanceof Number) {
			Number n = (Number) value;
			if (value instanceof Byte) {
				return IntLiteral.createNode("0x"+Integer.toHexString(n.byteValue()));
			}
			else if (value instanceof Short) {
				return IntLiteral.createNode(Short.toString(n.shortValue()));
			}
			else if (value instanceof Integer) {
				return IntLiteral.createNode(Integer.toString(n.intValue()));
			}
			else if (value instanceof Long) {
				return IntLiteral.createNode(Long.toString(n.longValue())+"L");
			}
			else if (value instanceof Float) {
				return FloatLiteral.createNode(Float.toString(n.floatValue())+"f");
			}
			else if (value instanceof Double) {
				return FloatLiteral.createNode(Double.toString(n.doubleValue()));
			}
			throw new IllegalStateException("Unexpected value: "+value);
		}
		else if (value instanceof Boolean) {		
			Boolean b = (Boolean) value;			
			return b ? TrueExpression.prototype.jjtCreate() : FalseExpression.prototype.jjtCreate();
		}
		else if (value instanceof Character) {
			Character c = (Character) value;
			return CharLiteral.createNode(c.toString());
		}
		else if (value instanceof Type) {
			Type t = (Type) value;			
			IRNode type = ClassAdapter.adaptTypeDescriptor(t.getDescriptor());
			return ClassExpression.createNode(type);
		}
		else if (value.getClass().isArray()) {		
			return adaptArrayValues(value);
		}
		throw new IllegalStateException("Unexpected value: "+value);
	}
	
	static IRNode adaptArrayValues(Object array) {
		final int len = Array.getLength(array);
		IRNode[] values = new IRNode[len];
		if (array instanceof long[]) {
			final long[] a = (long[]) array;
			for(int i=0; i<a.length; i++) {
				values[i] = adaptValue(a[i]);
			}
		}		
		else if (array instanceof boolean[]) {
			final boolean[] a = (boolean[]) array;
			for(int i=0; i<a.length; i++) {
				values[i] = adaptValue(a[i]);
			}
		}
		else if (array instanceof double[]) {
			final double[] a = (double[]) array;
			for(int i=0; i<a.length; i++) {
				values[i] = adaptValue(a[i]);
			}
		}
		else {		
			for(int i=0; i<len; i++) {
				values[i] = adaptValue(Array.get(array, i));
			}
		}
		return ArrayInitializer.createNode(values);
	}
	
	void add(String name, IRNode n) {
		if (n == null) {
			throw new NullPointerException();
		}
		pairs.add(ElementValuePair.createNode(name, n));
	}
	
	@Override
  public void visit(String name, Object value) {
		add(name, adaptValue(value));
	}

	@Override
  public AnnotationVisitor visitAnnotation(final String name, String desc) {
		final AnnoBuilder outer = this;
		return new AnnoBuilder(desc) {
			@Override
			public void visitEnd() {				
				super.visitEnd();
				outer.add(name, result);
			}
		};
	}

	@Override
  public AnnotationVisitor visitArray(String name) {
		return new ArrayBuilder(this, name);
	}

	@Override
  public void visitEnum(String name, String typeDesc, String value) {
		IRNode type = ClassAdapter.adaptTypeDescriptor(typeDesc);
		add(name, FieldRef.createNode(TypeExpression.createNode(type), value));
	}
	
	@Override
  public void visitEnd() {
		if (pairs.isEmpty()) {
			result = MarkerAnnotation.createNode(type);
		} else {
			IRNode pairs = ElementValuePairs.createNode(this.pairs.toArray(JavaGlobals.noNodes));
			result = NormalAnnotation.createNode(type, pairs);
		}
	}

	// Ignore all name parameters
	class ArrayBuilder extends AnnoBuilder {
		private final AnnoBuilder outer;
		private final String name;
		
		ArrayBuilder(AnnoBuilder o, String name) {
			super(null);
			outer = o;
			this.name = name;
		}

		@Override
		void add(String name, IRNode n) {
			pairs.add(n);
		}

		@Override
		public void visitEnd() {			
			if (pairs.isEmpty()) {
				// TODO is there some other way to know what this should be?
				result = ElementValueArrayInitializer.createNode(JavaGlobals.noNodes);
			}
			else if (Initializer.prototype.includes(pairs.get(0))) {
				result = ArrayInitializer.createNode(pairs.toArray(JavaGlobals.noNodes));
			}
			else {
				result = ElementValueArrayInitializer.createNode(pairs.toArray(JavaGlobals.noNodes));
			}
			outer.add(name, result);
		}	
	}
}