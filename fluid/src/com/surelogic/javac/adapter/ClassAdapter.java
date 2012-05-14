package com.surelogic.javac.adapter;

import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.objectweb.asm.*;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.EmptyVisitor;
import org.objectweb.asm.signature.*;

import com.surelogic.common.logging.SLLogger;
import com.surelogic.javac.Util;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.java.*;
import edu.cmu.cs.fluid.java.adapter.AbstractAdapter;
import edu.cmu.cs.fluid.java.adapter.CodeContext;
import edu.cmu.cs.fluid.java.operator.*;

public class ClassAdapter extends AbstractAdapter implements ClassVisitor {	
	final boolean debug;
	final ZipFile jar;
	final String className;
	final File classFile;
	final ClassResource resource;
	
	final boolean isInner;
	final int mods; // Only applicable if nested
	int access;
	String name;
	IRNode[] formals = noNodes;
	//String signature;
	IRNode superType;
	IRNode[] ifaces = noNodes;
	IRNode root;
	final List<IRNode> members = new ArrayList<IRNode>();
	
	public ClassAdapter(String project, ZipFile j, String qname, boolean inner, int mods) {
		super(SLLogger.getLogger());
		jar = j;
		className = qname.replace('.', '/')+".class";
		isInner   = inner;
		classFile = null;
		debug     = Util.debug;
		//debug     = "java/lang/Enum.class".equals(cls); 
		this.mods = mods;
		resource = new ClassResource(project, qname);
	}

	public ClassAdapter(String project, File f, String qname, boolean inner, int mods) {
		super(SLLogger.getLogger());
		jar = null;
		className = null;
		classFile = f;
		isInner   = inner;
		debug     = Util.debug;
		this.mods = mods;
		resource = new ClassResource(project, qname, f);
	}
	
	public File getSource() {
		if (jar == null) {
			return classFile;
		}
		return new File(jar.getName());
	}
	
	public IRNode getRoot() throws IOException {
		final String label;
		InputStream is = null;
		if (jar != null) {
			label = className;
			ZipEntry e = jar.getEntry(className);
			if (e != null) {
				is = jar.getInputStream(e);
			}
		} else {
			label = classFile.getAbsolutePath();
			is = new FileInputStream(classFile);
		}
		if (is != null) {
			ClassReader cr = new ClassReader(is);
			cr.accept(this, 0);
			/*
			cr = new ClassReader(jar.getInputStream(e));
			cr.accept(new AnalyzeSignaturesVisitor(), 0);
			*/
			final ISrcRef ref = new ClassRef(resource, 0);
			JavaNode.setSrcRef(root, ref);
			
			if (!isInner) {
				// Need to create comp unit
				IRNode annos = edu.cmu.cs.fluid.java.operator.Annotations.createNode(noNodes); // FIX
				int lastSlash = name.lastIndexOf('/');
				String id;
				if (lastSlash < 0) {
					id = name;
				} else {
					id = name.substring(0, lastSlash);
				}
				id = id.replace('/', '.');
				/*
				if ("1".equals(id)) {
					System.out.println("Got synthetic class: "+id);
				}
               */
				id = CommonStrings.intern(id);
				
				IRNode pkg   = NamedPackageDeclaration.createNode(annos, id);
				IRNode imps  = ImportDeclarations.createNode(noNodes);
				IRNode decls = TypeDeclarations.createNode(new IRNode[] { root });
				IRNode cu    = CompilationUnit.createNode(pkg, imps, decls);
				createLastMinuteNodes(cu, true, resource.getProject());
				JavaNode.setModifiers(cu, JavaNode.AS_BINARY);
                /*
				if ("java/lang/Object".equals(name)) {				
				  System.out.println("Done adapting "+JavaNames.getFullTypeName(root)+": "+root);
				//System.out.println(DebugUnparser.toString(cu));
				}
                */
				return cu;
			}
			return root;
		} else {
			System.err.println("ClassAdapter couldn't find "+label);
		}
		return null;
	}
	
	public void visit(int version, int access, String name,
			String sig, String sname, String[] interfaces) {
		if (debug) {
			if (isInner) {
				System.out.println("Adapting inner class "+name);
			} else {
				System.out.println("Adapting class "+name);
			}
		}
		/*
		if (debug) {
			System.out.println(sig);
		}
		*/
		this.access = access;
		this.name = name;
		if (sig == null) {
			if (sname != null) {
				superType = adaptTypeName(sname);
			}
			ifaces = map(adaptTypeName, interfaces, null); 
		} else {
			TypeDeclVisitor tdv = new TypeDeclVisitor();
			new SignatureReader(sig).accept(tdv);
			ifaces = tdv.getInterfaces();
		}
	}

	private class TypeDeclVisitor extends TypeFormalVisitor {
		List<IRNode> interfaces = new ArrayList<IRNode>();
		public SignatureVisitor visitSuperclass() {
			buildFormal();
			ClassAdapter.this.formals = formals.toArray(noNodes);
			return new TypeVisitor() {
				protected void finish() {
					superType = this.getType();
				}
			};
		}	
		// One or more times
		public SignatureVisitor visitInterface() {
			return new TypeVisitor() {
				protected void finish() {
					interfaces.add(this.getType());
				}
			};
		}
		public IRNode[] getInterfaces() {
			return interfaces.toArray(noNodes);
		}
	}
	
	public void visitSource(String source, String debug) {
	}

	public void visitOuterClass(String owner, String name, String desc) {
	}

	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
		return null; // FIX
	}

	public void visitAttribute(Attribute attr) {
	}

	public void visitInnerClass(String name, String outerName,
			String innerName, int access) {
		if (innerName == null || !this.name.equals(outerName)) {
			return;
		}
		if (innerName.length() == 0) {
			return;
		}
		if (Character.isDigit(innerName.charAt(0))) {
			// A generated class, starting with a digit
			return;
		}
		final int mods = adaptModifiers(access);
		ClassAdapter a;
		if (jar != null) {
			a = new ClassAdapter(resource.getProject(), jar, name.replace('/', '.'), true, mods);
		} else {
			int lastSlash = name.lastIndexOf('/');
			if (lastSlash >= 0) {
				name = name.substring(lastSlash+1);
			}
			String className = name+".class";
			a = new ClassAdapter(resource.getProject(), 
					             new File(classFile.getParentFile(), className), 
					             name.replace('/', '.'), true, mods);
		}
		try {
			IRNode result = a.getRoot();
			if (result != null) {
				JavaNode.setModifiers(result, mods);
				members.add(result);
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new UnsupportedOperationException("Could make inner class: "+name);
		}
	}

	public FieldVisitor visitField(int access, String name, String desc,
			String signature, Object value) {
		if ((access & Opcodes.ACC_PRIVATE) != 0) {
			return null;
		}
		if (isEnumDecl()) {
			IRNode result = SimpleEnumConstantDeclaration.createNode(name);
			members.add(result);
		} else {
			IRNode annos = edu.cmu.cs.fluid.java.operator.Annotations.createNode(noNodes); // FIX
			int mods     = adaptModifiers(access);
			IRNode type;
			if (signature == null) {
				type = adaptTypeDescriptor(desc);
			} else {
				//new SignatureReader(signature).accept(new TraceSignatureVisitor("  "));			
				FieldDeclVisitor tv = new FieldDeclVisitor();
				new SignatureReader(signature).accept(tv);
				type = tv.getType();
			}			
			IRNode vdecl = VariableDeclarator.createNode(name, 0, 
					NoInitialization.prototype.jjtCreate());
			IRNode vars  = VariableDeclarators.createNode(new IRNode[] { vdecl });
			IRNode result = FieldDeclaration.createNode(annos, mods, type, vars);
			members.add(result);
		}
		return null; // FIX for annos
	}

	private class FieldDeclVisitor extends TypeVisitor {
		@Override	
		public SignatureVisitor visitSuperclass() {
			return this;
		}
	}
	
	private class MethodDeclVisitor extends TypeFormalVisitor {
		final boolean varargs;
		final int expectedParams;
		IRNode[] types; 
		final List<IRNode> paramTypes;
		IRNode rType = null; // illegal;
		final List<IRNode> exTypes = new ArrayList<IRNode>();
		
		public MethodDeclVisitor(boolean vargs, int numParams) {
			varargs = vargs;
			expectedParams = numParams;
			paramTypes = new ArrayList<IRNode>(numParams);
		}
		public SignatureVisitor visitParameterType() {
			return new TypeVisitor(varargs && paramTypes.size() == expectedParams-1) {
				protected void finish() {
					paramTypes.add(this.getType());
				}
			};
		}
		public SignatureVisitor visitReturnType() {
			//System.out.println("visitReturnType()");
			buildFormal();
			types = formals.toArray(noNodes);
			return new TypeVisitor() {
				protected void finish() {
					if (this.getType() == null) {
						System.out.println("null getType()");
					}
					rType = this.getType();
				}
			};
		}
		public SignatureVisitor visitExceptionType() {
			return new TypeVisitor() {
				protected void finish() {
					exTypes.add(this.getType());
				}
			};
		}
	}
	
	private <T> IRNode createParameters(T[] paramTypes, Function<T> func, 
			                            boolean skipFirstArg) {
		final int num  = paramTypes.length;
		/*
		if (num == 0 && skipFirstArg) {
			System.out.println("No args to skip");
		}
		*/
		final int size = skipFirstArg ? num-1 : num;
		IRNode[] nodes = new IRNode[size];
		for(int i = 0; i<size; i++) {
			final int index = skipFirstArg ? i+1 : i;
			IRNode pAnnos = edu.cmu.cs.fluid.java.operator.Annotations.createNode(noNodes); // FIX
			IRNode type   = func.call(paramTypes[index], null, index, size);
			String id     = getDummyArg(index);
			nodes[i] = ParameterDeclaration.createNode(pAnnos, 0, type, id);
		}
		return Parameters.createNode(nodes);
	}
	
	private final Function<Type> adaptTypeDescriptor = new AbstractFunction<Type>() {
		public IRNode call(Type t, CodeContext context) {
			return adaptTypeDescriptor(t.getDescriptor(), false);
		}
	};
	private final Function<Type> adaptVarargsTypeDescriptor = new Function<Type>() {
		public IRNode call(Type t, CodeContext context, int i, int n) {
			return adaptTypeDescriptor(t.getDescriptor(), i == n-1);
		}
	};
	private final Function<IRNode> identity = new AbstractFunction<IRNode>() {
		public IRNode call(IRNode t, CodeContext context) {
			return t;
		}
	};
	
	/**
	 * signature includes generics if any
	 * desc is the raw thing
	 */
	public MethodVisitor visitMethod(int access, String name,
			String desc, String signature, String[] exceptions) {
		if (debug) {
			System.out.println("Visiting "+name+" "+desc+" "+signature);
		}
		if ("<clinit>".equals(name)) {
			return null;
		}
		final boolean isConstructor = "<init>".equals(name);
		/*
        if (debug && isConstructor) {
			System.out.println(desc);
		}
		*/
		final boolean isSynthetic = (access & Opcodes.ACC_SYNTHETIC) != 0;
		if (!isConstructor) {
			if ((access & Opcodes.ACC_PRIVATE) != 0) {
				return null;
			}
			if (isSynthetic) {
				return null;
			}
		}
		if ((access & Opcodes.ACC_BRIDGE) != 0) {
			return null;
		}
		/*
		if (debug && "<init>".equals(name)) {
			System.out.println(signature);
		}
		else if (debug && "clone".equals(name)) {
			System.out.println("clone sig: "+signature);
		}
		*/
		IRNode types, params, exs;
		IRNode rType = null;
		MethodDeclVisitor mdv = null;
		final boolean varargs = (access & Opcodes.ACC_VARARGS) != 0;	
		Type[] paramTypes = Type.getArgumentTypes(desc);
		// Check if non-static nested class
		// desc (not signature) contains an extra arg for the ref to the outer class
		// If so, constructor needs to be modified to make the first arg implicit		
		final boolean skipFirstArg = isConstructor && isInner && !JavaNode.isSet(this.mods, JavaNode.STATIC);
		/*
		if (signature != null && !signature.equals(desc) && skipFirstArg) {			
			System.out.println("signature and desc not the same: "+desc);
		}
		*/
		if (signature == null) {
			/*
			if (skipFirstArg) {
				System.out.println("Skipping first arg: "+desc);
			}
			*/
			types = TypeFormals.createNode(noNodes);
		    exs   = Throws.createNode(map(adaptTypeName, exceptions, null));
			if (desc != null) {
				params = createParameters(paramTypes, varargs ? adaptVarargsTypeDescriptor :
					                                            adaptTypeDescriptor, skipFirstArg);
			} else {
				params = Parameters.createNode(noNodes);		
			}
		} else {
			mdv = new MethodDeclVisitor(varargs, paramTypes.length);
			new SignatureReader(signature).accept(mdv);
			types  = TypeFormals.createNode(mdv.types);
	        // signature doesn't contain the extra arg, so no need to skip
			params = createParameters(mdv.paramTypes.toArray(noNodes), identity, false);
			rType  = mdv.rType;			
			exs    = Throws.createNode(mdv.exTypes.toArray(noNodes));
			
			if (rType == null) {
				new SignatureReader(signature).accept(new TraceSignatureVisitor("  "));	
			}
		}
		final IRNode result;
		if (isAnnotationDecl()) {
			// See below
			String rName = desc.substring(desc.lastIndexOf(')')+1, desc.length());
			if (signature == null) {
				rType = adaptTypeDescriptor(rName);
			}
			if (rType == null) {
				System.out.println("Null type for anno elt");
			}
			result = AnnotationElement.createNode(JavaNode.PUBLIC, rType, name, params, exs, 
					NoMethodBody.prototype.jjtCreate(), 
					NoDefaultValue.prototype.jjtCreate());
		} else {
			IRNode annos  = edu.cmu.cs.fluid.java.operator.Annotations.createNode(noNodes); // FIX
			int mods      = adaptModifiers(access);
			IRNode body   = CompiledMethodBody.createNode("no body");
			if (isConstructor) {
				int delim = this.name.lastIndexOf('$');
				if (delim < 0) {							
					delim = this.name.lastIndexOf('/');
				}
				String cname = delim < 0 ? this.name : this.name.substring(delim+1);
				result = ConstructorDeclaration.createNode(annos, mods, types, cname, params, 
						exs, body);
			} else {
				String rName = desc.substring(desc.lastIndexOf(')')+1, desc.length());
				if (signature == null) {
					rType = adaptTypeDescriptor(rName);
				}
				if (rType == null) {
					System.out.println("null rType");
				}
				result = MethodDeclaration.createNode(annos, mods, types, rType, name, params, 
						0, exs, body);
			}
			createRequiredMethodNodes(JavaNode.isSet(mods, JavaNode.STATIC), result);
		}
		members.add(result);
		
		final IRNode parameters = params;
		// Used to get the first line number
		return new EmptyVisitor() {
			int line = Integer.MAX_VALUE;
			
			@Override
			public void visitLineNumber(int newLine, Label label) {
				if (newLine < line) {
					line = newLine;
				}
			}
			
			@Override
			public void visitEnd() {
				//System.out.println("Got line#"+line);
				if (line == Integer.MAX_VALUE) {
					line = 0;
				}
				final ISrcRef ref = new ClassRef(resource, line);
				JavaNode.setSrcRef(result, ref);				
				for(IRNode p : Parameters.getFormalIterator(parameters)) {
					JavaNode.setSrcRef(p, ref);
				}
			}
		}; // FIX for annos/line numbers
	}

	public void visitEnd() {
		String id;
		int separator = name.lastIndexOf('/');
		int dollar    = name.lastIndexOf('$');
		if (separator < dollar) {
			separator = dollar;
		}
		if (separator < 0) {
			id = name;
		} else {
			id = name.substring(separator+1);
		}
		/*
		if (id.equals("ThreadLocal")) {
			System.out.println(root);			
		}
		*/
		int mods     = adaptModifiers(access);
		IRNode annos = edu.cmu.cs.fluid.java.operator.Annotations.createNode(noNodes); // FIX
		IRNode types = TypeFormals.createNode(formals); 
		IRNode body  = ClassBody.createNode(members.toArray(noNodes));
		/*
		if ("State".equals(id)) {
			System.out.println("Looking at "+name);
		}
		*/
		
		if (isAnnotationDecl()) {
			if (isInner) {
				root = NestedAnnotationDeclaration.createNode(annos, mods, id, body);
			} else {
				root = AnnotationDeclaration.createNode(annos, mods, id, body);
			}
		} 
		else if ((access & Opcodes.ACC_INTERFACE) != 0) {
			IRNode extensions = Extensions.createNode(ifaces);
			if (isInner) {
				root = NestedInterfaceDeclaration.createNode(annos, mods, id, types, extensions, body);
			} else {
				root = InterfaceDeclaration.createNode(annos, mods, id, types, extensions, body);
			}
		}
		else if (isEnumDecl()) {
			IRNode impls = Implements.createNode(ifaces);
			if (isInner) {
				root = NestedEnumDeclaration.createNode(annos, mods, id, impls, body);
			} else {
				root = EnumDeclaration.createNode(annos, mods, id, impls, body);
			}
		}
		else {
			IRNode ext   = superType == null ? NamedType.createNode("java.lang.Object") : superType;
			IRNode impls = Implements.createNode(ifaces);
			/*
			if ("1".equals(id)) {
				System.out.println("Unusual class name: "+id);
			}
			*/
			if (isInner) {
				root = NestedClassDeclaration.createNode(annos, mods, id, types, ext, impls, body);
			} else {
				root = ClassDeclaration.createNode(annos, mods, id, types, ext, impls, body);
			}
		}
	}
	
	private boolean isEnumDecl() {
		return (access & Opcodes.ACC_ENUM) != 0;
	}
	
	private boolean isAnnotationDecl() {
		return (access & Opcodes.ACC_ANNOTATION) != 0;
	}
	
	private final Function<String> adaptTypeName = new AbstractFunction<String>() {
		public IRNode call(String t, CodeContext context) {
			return adaptTypeName(t);
		}
	};
	
	public static IRNode adaptTypeName(String name) {
		if (name == null) {
			throw new NullPointerException();
		}
		int dollar = name.lastIndexOf('$');
		if (dollar < 0) {
			String qname = name.replace('/', '.');
			qname = CommonStrings.intern(qname);
			return NamedType.createNode(qname);
		}
		IRNode base = adaptTypeName(name.substring(0, dollar));
		return TypeRef.createNode(base, name.substring(dollar+1));
	}
	
	private IRNode adaptTypeDescriptor(String desc) {
		return adaptTypeDescriptor(desc, 0);
	}	
	private IRNode adaptTypeDescriptor(String desc, boolean varargs) {
		if (varargs) {
			if (desc.charAt(0) != '[') {
				throw new IllegalArgumentException("Non-array: "+desc);
			}
			IRNode base = adaptTypeDescriptor(desc, 1);
			return VarArgsType.createNode(base);
		} else {
			return adaptTypeDescriptor(desc, 0);
		}
	}
	private IRNode adaptTypeDescriptor(String desc, int i) {
		switch (desc.charAt(i)) {
		case 'V':
			return VoidType.prototype.jjtCreate();
		case 'Z':
			return BooleanType.prototype.jjtCreate();
		case 'B':
			return ByteType.prototype.jjtCreate();
		case 'C':
			return CharType.prototype.jjtCreate();
		case 'S':
			return ShortType.prototype.jjtCreate();
		case 'I':
			return IntType.prototype.jjtCreate();
		case 'J':
			return LongType.prototype.jjtCreate();
		case 'F':
			return FloatType.prototype.jjtCreate();
		case 'D':
			return DoubleType.prototype.jjtCreate();
		case 'L':
			int semi = desc.indexOf(';', i);
			return adaptTypeName(desc.substring(i+1, semi));
		case '[':
			int j = i+1;
			while (j<desc.length() && desc.charAt(j) == '[') {				
				j++;
			}
			return ArrayType.createNode(adaptTypeDescriptor(desc, j), j-i);
		default:
			throw new IllegalArgumentException("Couldn't parse "+desc);				
		}
	}
	
	public int adaptModifiers(int access) {
		int mods = JavaNode.AS_BINARY;
		if ((access & Opcodes.ACC_ABSTRACT) != 0) {
			mods = JavaNode.setModifier(mods, JavaNode.ABSTRACT, true);
		}
		if ((access & Opcodes.ACC_FINAL) != 0) {
			mods = JavaNode.setModifier(mods, JavaNode.FINAL, true);
		}
		if ((access & Opcodes.ACC_NATIVE) != 0) {
			mods = JavaNode.setModifier(mods, JavaNode.NATIVE, true);
		}
		if ((access & Opcodes.ACC_PRIVATE) != 0) {
			mods = JavaNode.setModifier(mods, JavaNode.PRIVATE, true);
		}
		if ((access & Opcodes.ACC_PROTECTED) != 0) {
			mods = JavaNode.setModifier(mods, JavaNode.PROTECTED, true);
		}
		if ((access & Opcodes.ACC_PUBLIC) != 0) {
			mods = JavaNode.setModifier(mods, JavaNode.PUBLIC, true);
		}
		if ((access & Opcodes.ACC_STATIC) != 0) {
			mods = JavaNode.setModifier(mods, JavaNode.STATIC, true);
		}
		if ((access & Opcodes.ACC_STRICT) != 0) {
			mods = JavaNode.setModifier(mods, JavaNode.STRICTFP, true);
		}
		if ((access & Opcodes.ACC_SYNCHRONIZED) != 0) {
			mods = JavaNode.setModifier(mods, JavaNode.SYNCHRONIZED, true);
		}
		if ((access & Opcodes.ACC_TRANSIENT) != 0) {
			mods = JavaNode.setModifier(mods, JavaNode.TRANSIENT, true);
		}
		if ((access & Opcodes.ACC_VOLATILE) != 0) {
			mods = JavaNode.setModifier(mods, JavaNode.VOLATILE, true);
		}
		return mods;
	}
}
