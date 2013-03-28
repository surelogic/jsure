package com.surelogic.javac.adapter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.collections15.MultiMap;
import org.apache.commons.collections15.multimap.MultiHashMap;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;

import com.surelogic.common.SLUtility;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.javac.JavacProject;
import com.surelogic.javac.Util;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.CommonStrings;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.SkeletonJavaRefUtility;
import edu.cmu.cs.fluid.java.adapter.*;
import edu.cmu.cs.fluid.java.operator.AnnotationDeclaration;
import edu.cmu.cs.fluid.java.operator.AnnotationElement;
import edu.cmu.cs.fluid.java.operator.Annotations;
import edu.cmu.cs.fluid.java.operator.ArrayType;
import edu.cmu.cs.fluid.java.operator.BooleanType;
import edu.cmu.cs.fluid.java.operator.ByteType;
import edu.cmu.cs.fluid.java.operator.CharType;
import edu.cmu.cs.fluid.java.operator.ClassBody;
import edu.cmu.cs.fluid.java.operator.ClassDeclaration;
import edu.cmu.cs.fluid.java.operator.CompilationUnit;
import edu.cmu.cs.fluid.java.operator.CompiledMethodBody;
import edu.cmu.cs.fluid.java.operator.ConstructorDeclaration;
import edu.cmu.cs.fluid.java.operator.DoubleType;
import edu.cmu.cs.fluid.java.operator.EnumDeclaration;
import edu.cmu.cs.fluid.java.operator.Extensions;
import edu.cmu.cs.fluid.java.operator.FieldDeclaration;
import edu.cmu.cs.fluid.java.operator.FloatType;
import edu.cmu.cs.fluid.java.operator.Implements;
import edu.cmu.cs.fluid.java.operator.ImpliedEnumConstantInitialization;
import edu.cmu.cs.fluid.java.operator.ImportDeclarations;
import edu.cmu.cs.fluid.java.operator.IntType;
import edu.cmu.cs.fluid.java.operator.InterfaceDeclaration;
import edu.cmu.cs.fluid.java.operator.LongType;
import edu.cmu.cs.fluid.java.operator.MethodDeclaration;
import edu.cmu.cs.fluid.java.operator.NamedPackageDeclaration;
import edu.cmu.cs.fluid.java.operator.NamedType;
import edu.cmu.cs.fluid.java.operator.NestedAnnotationDeclaration;
import edu.cmu.cs.fluid.java.operator.NestedClassDeclaration;
import edu.cmu.cs.fluid.java.operator.NestedEnumDeclaration;
import edu.cmu.cs.fluid.java.operator.NestedInterfaceDeclaration;
import edu.cmu.cs.fluid.java.operator.NoDefaultValue;
import edu.cmu.cs.fluid.java.operator.NoInitialization;
import edu.cmu.cs.fluid.java.operator.NoMethodBody;
import edu.cmu.cs.fluid.java.operator.ParameterDeclaration;
import edu.cmu.cs.fluid.java.operator.Parameters;
import edu.cmu.cs.fluid.java.operator.ShortType;
import edu.cmu.cs.fluid.java.operator.SimpleEnumConstantDeclaration;
import edu.cmu.cs.fluid.java.operator.Throws;
import edu.cmu.cs.fluid.java.operator.TypeDeclarations;
import edu.cmu.cs.fluid.java.operator.TypeFormals;
import edu.cmu.cs.fluid.java.operator.TypeRef;
import edu.cmu.cs.fluid.java.operator.VarArgsType;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.operator.VariableDeclarators;
import edu.cmu.cs.fluid.java.operator.VoidType;
import edu.cmu.cs.fluid.java.util.DeclFactory;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.Triple;

public class ClassAdapter extends AbstractAdapter {
  private static final String PACKAGE_INFO_CLASS = "package-info.class";
	
  final boolean debug;
  final ZipFile jar;
  final String className;
  final File classFile;
  final ClassResource resource;
  final DeclFactory declFactory;

  final boolean isInner;
  final int mods; // Only applicable if nested
  int access;
  String name;
  IRNode[] formals = noNodes;
  // String signature;
  IRNode superType;
  IRNode[] ifaces = noNodes;
  /**
   * The type represented by the .class file
   */
  IRNode root;
  final List<IRNode> annos = new ArrayList<IRNode>();
  final List<IRNode> members = new ArrayList<IRNode>();

  public ClassAdapter(JavacProject project, ZipFile j, String qname, boolean inner, int mods) {
    super(SLLogger.getLogger());
    jar = j;
    className = qname.replace('.', '/') + ".class";
    isInner = inner;
    classFile = null;
    debug = Util.debug;
    // debug = "java/lang/Enum.class".equals(cls);
    this.mods = mods;
    resource = new ClassResource(project, qname, j.getName(), className);
    declFactory = new DeclFactory(project.getTypeEnv().getBinder());
  }

  public ClassAdapter(JavacProject project, File f, String qname, boolean inner, int mods) {
    super(SLLogger.getLogger());
    jar = null;
    className = qname.replace('.', '/') + ".class";
    classFile = f;
    isInner = inner;
    debug = Util.debug;
    this.mods = mods;
    resource = new ClassResource(project, qname, f);
    declFactory = new DeclFactory(project.getTypeEnv().getBinder());
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
      cr.accept(new Visitor(), 0);
      /*
       * cr = new ClassReader(jar.getInputStream(e)); cr.accept(new
       * AnalyzeSignaturesVisitor(), 0);
       */
      if (root != null) {
    	  SkeletonJavaRefUtility.registerBinaryCode(declFactory, root, resource, 0);
    	  for(IRNode a : annos) {
    		  SkeletonJavaRefUtility.copyIfPossible(root, a);
    	  }
    	  for(IRNode v : VisitUtil.getClassFieldDeclarators(root)) {
    		  SkeletonJavaRefUtility.copyIfPossible(root, v);
    	  }
    	  final Operator op = JJNode.tree.getOperator(root);
    	  IRNode formals = null;
    	  if (ClassDeclaration.prototype.includes(op)) {
    		  formals = ClassDeclaration.getTypes(root);
    	  }
    	  else if (InterfaceDeclaration.prototype.includes(op)) {
    		  formals = InterfaceDeclaration.getTypes(root);
    	  }
    	  if (formals != null) {
    		  for(IRNode f : TypeFormals.getTypeIterator(formals)) {
    			  SkeletonJavaRefUtility.copyIfPossible(root, f);
    		  }
    	  }
      }
      if (!isInner) {
        // Need to create comp unit
        int lastSlash = name.lastIndexOf('/');
        String id;
        if (lastSlash < 0) {
          id = "";
        } else {
          id = name.substring(0, lastSlash);
        }
        id = id.replace('/', '.');
        /*
         * if ("1".equals(id)) { System.out.println("Got synthetic class: "+id);
         * }
         */
        id = CommonStrings.intern(id);
        final IRNode annos, decls;
        if (className.endsWith(PACKAGE_INFO_CLASS)) {
        	annos = edu.cmu.cs.fluid.java.operator.Annotations.createNode(this.annos.toArray(noNodes));
            decls = TypeDeclarations.createNode(noNodes);
    	} else {
    		annos = edu.cmu.cs.fluid.java.operator.Annotations.createNode(noNodes);
            decls = TypeDeclarations.createNode(new IRNode[] { root });
    	}
        IRNode pkg = NamedPackageDeclaration.createNode(annos, id);
        IRNode imps = ImportDeclarations.createNode(noNodes);
        IRNode cu = CompilationUnit.createNode(pkg, imps, decls);
        createLastMinuteNodes(cu, true, resource.getProjectName());
        JavaNode.setModifiers(cu, JavaNode.AS_BINARY);
        /*
         * if ("java/lang/Object".equals(name)) {
         * System.out.println("Done adapting "
         * +JavaNames.getFullTypeName(root)+": "+root);
         * //System.out.println(DebugUnparser.toString(cu)); }
         */
        return cu;
      }
      return root;
    } else {
      System.err.println("ClassAdapter couldn't find " + label);
    }
    return null;
  }

  public void visit(int version, int access, String name, String sig, String sname, String[] interfaces) {
    if (debug) {
      if (isInner) {
        System.out.println("Adapting inner class " + name);
      } else {
        System.out.println("Adapting class " + name);
      }
    }
    /*
     * if (debug) { System.out.println(sig); }
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

  public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
    return new AnnoBuilder(desc) {
      @Override
      public void visitEnd() {
        super.visitEnd();
        annos.add(result);
        // System.out.println("Added @"+DebugUnparser.toString(result));
      }
    };
  }

  public void visitInnerClass(String name, String outerName, String innerName, int access) {
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
        name = name.substring(lastSlash + 1);
      }
      String className = name + ".class";
      a = new ClassAdapter(resource.getProject(), new File(classFile.getParentFile(), className), name.replace('/', '.'), true,
          mods);
    }
    try {
      IRNode result = a.getRoot();
      if (result != null) {
        JavaNode.setModifiers(result, mods);
        members.add(result);
      }
    } catch (IOException e) {
      e.printStackTrace();
      throw new UnsupportedOperationException("Could make inner class: " + name);
    }
  }

  public FieldVisitor visitField(final int access, final String name, final String desc, final String signature, Object value) {
    if ((access & Opcodes.ACC_PRIVATE) != 0) {
      return null;
    }
    return new FieldVisitor(Opcodes.ASM4) {
      final List<IRNode> annoList = new ArrayList<IRNode>();

      @Override
      public AnnotationVisitor visitAnnotation(String typeDesc, boolean viz) {
        return new AnnoBuilder(typeDesc) {
          @Override
          public void visitEnd() {
            super.visitEnd();
            annoList.add(result);
          }
        };
      }

      @Override
      public void visitEnd() {
        /*
         * if (annoList.size() > 0) {
         * System.out.println("Adding annos for "+name); }
         */
        IRNode annos = edu.cmu.cs.fluid.java.operator.Annotations.createNode(annoList.toArray(noNodes));
        if (isEnumDecl()) {
          final String typeName = className.substring(0, className.length() - 6);
          if (desc.startsWith("L") && desc.endsWith(";") && desc.length() == typeName.length() + 2
              && desc.regionMatches(1, typeName, 0, typeName.length())) {
            IRNode impliedInit = ImpliedEnumConstantInitialization.prototype.jjtCreate();
            IRNode result = SimpleEnumConstantDeclaration.createNode(annos, name, impliedInit);
            members.add(result);
            return;
          } else {
            // System.out.println("Found a field of type "+desc+" in "+typeName);
          }
        }
        int mods = adaptModifiers(access);
        IRNode type;
        if (signature == null) {
          type = adaptTypeDescriptor(desc);
        } else {
          // new SignatureReader(signature).accept(new
          // TraceSignatureVisitor("  "));
          FieldDeclVisitor tv = new FieldDeclVisitor();
          new SignatureReader(signature).accept(tv);
          type = tv.getType();
        }
        IRNode vdecl = VariableDeclarator.createNode(name, 0, NoInitialization.prototype.jjtCreate());
        IRNode vars = VariableDeclarators.createNode(new IRNode[] { vdecl });
        IRNode result = FieldDeclaration.createNode(annos, mods, type, vars);
        members.add(result);
      }
    };

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
      return new TypeVisitor(varargs && paramTypes.size() == expectedParams - 1) {
        protected void finish() {
          paramTypes.add(this.getType());
        }
      };
    }

    public SignatureVisitor visitReturnType() {
      // System.out.println("visitReturnType()");
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

  private <T> IRNode createParameters(MultiMap<Integer, IRNode> paramAnnos, T[] paramTypes, Function<T> func, boolean skipFirstArg) {
    final int num = paramTypes.length;
    /*
     * if (num == 0 && skipFirstArg) { System.out.println("No args to skip"); }
     */
    final int size = skipFirstArg ? num - 1 : num;
    IRNode[] nodes = new IRNode[size];
    for (int i = 0; i < size; i++) {
      final int index = skipFirstArg ? i + 1 : i;
      Collection<IRNode> annoList = paramAnnos.get(index);
      final IRNode[] annos;
      if (annoList == null) {
        annos = noNodes;
      } else {
        annos = annoList.toArray(noNodes);
      }
      IRNode pAnnos = edu.cmu.cs.fluid.java.operator.Annotations.createNode(annos);
      IRNode type = func.call(paramTypes[index], null, index, num);
      String id = getDummyArg(index);
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
      return adaptTypeDescriptor(t.getDescriptor(), i == n - 1);
    }
  };
  private final Function<IRNode> identity = new AbstractFunction<IRNode>() {
    public IRNode call(IRNode t, CodeContext context) {
      return t;
    }
  };

  /**
   * signature includes generics if any desc is the raw thing
   */
  public MethodVisitor visitMethod(final int access, final String name, final String desc, final String signature,
      final String[] exceptions) {
    if (debug) {
      System.out.println("Visiting " + name + " " + desc + " " + signature);
    }
    if ("<clinit>".equals(name)) {
      return null;
    }
    final boolean isConstructor = "<init>".equals(name);
    /*
     * if (debug && isConstructor) { System.out.println(desc); }
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
     * if (debug && "<init>".equals(name)) { System.out.println(signature); }
     * else if (debug && "clone".equals(name)) {
     * System.out.println("clone sig: "+signature); }
     */
    final boolean varargs = (access & Opcodes.ACC_VARARGS) != 0;

    // Check if non-static nested class
    // desc (not signature) contains an extra arg for the ref to the outer class
    // If so, constructor needs to be modified to make the first arg implicit
    final boolean skipFirstArg = isConstructor && isInner && !JavaNode.isSet(this.mods, JavaNode.STATIC);
    /*
     * if (signature != null && !signature.equals(desc) && skipFirstArg) {
     * System.out.println("signature and desc not the same: "+desc); }
     */
    // Handles line numbers and annotations
    return new MethodVisitor(Opcodes.ASM4) {
    	int line = Integer.MAX_VALUE;
    	final List<IRNode> annoList = new ArrayList<IRNode>();
    	final MultiMap<Integer, IRNode> paramAnnos = new MultiHashMap<Integer, IRNode>();

    	@Override
    	public void visitLineNumber(int newLine, Label label) {
    		if (newLine < line) {
    			line = newLine;
    		}
    	}
    	
    	@Override
    	public AnnotationVisitor visitAnnotation(String desc, boolean viz) {
    		return new AnnoBuilder(desc) {
    			@Override
    			public void visitEnd() {
    				super.visitEnd();
    				annoList.add(result);
    			}
    		};
    	}

    	@Override
    	public AnnotationVisitor visitParameterAnnotation(final int i, String desc, boolean viz) {
    		return new AnnoBuilder(desc) {
    			@Override
    			public void visitEnd() {
    				super.visitEnd();
    				paramAnnos.put(i, result);
    			}
    		};
    	}
    	
    	@Override
    	public void visitEnd() {
    		final IRNode annos = Annotations.createNode(annoList.toArray(noNodes));
    		final IRNode result, parameters;
    		final boolean isStatic;
    		if (isAnnotationDecl()) {
    			result = makeAnnoElement(annos);
    			parameters = null;
    			isStatic = false;
    		} else {
    			final Triple<IRNode,IRNode,Boolean> info = makeFunction(annos);
    			result = info.first();
    			parameters = info.second();
    			isStatic = info.third();
    		}
 			addRefs(result, parameters, annos);
    		createRequiredMethodNodes(isStatic, result);
    		members.add(result);
    	}

    	private IRNode makeAnnoElement(IRNode annos) {
    		IRNode params = Parameters.createNode(noNodes);
    		// See below
    		String rName = desc.substring(desc.lastIndexOf(')') + 1, desc.length());
    		IRNode rType = null;
    		if (signature == null) {
    			rType = adaptTypeDescriptor(rName);
    		} else {
    			MethodDeclVisitor mdv = new MethodDeclVisitor(false, 0);
    			new SignatureReader(signature).accept(mdv);
    			rType = mdv.rType;
    		}
    		if (rType == null) {
    			System.out.println("Null type for anno elt");
    		}
    		IRNode exs = Throws.createNode(noNodes);
    		IRNode result = AnnotationElement.createNode(annos, JavaNode.PUBLIC | JavaNode.ABSTRACT, 
    				rType, name, params, exs, NoMethodBody.prototype.jjtCreate(),
    				NoDefaultValue.prototype.jjtCreate());
    		return result;
    	};
		
    	private Triple<IRNode,IRNode,Boolean> makeFunction(IRNode annos) {
    		final int mods = adaptModifiers(access);
    		final IRNode body = CompiledMethodBody.createNode("no body");
    		final String className = ClassAdapter.this.name;

    		final IRNode types, exs;
    		final Type[] paramTypes = Type.getArgumentTypes(desc);
    		IRNode rType = null;
    		final IRNode parameters;
    		if (signature == null) {
    			/*
    			 * if (skipFirstArg) {
    			 * System.out.println("Skipping first arg: "+desc); }
    			 */
    			types = TypeFormals.createNode(noNodes);
    			exs = Throws.createNode(map(adaptTypeName, exceptions, null));
    			if (desc != null) {
    				parameters = createParameters(paramAnnos, paramTypes, 
    						varargs ? adaptVarargsTypeDescriptor : adaptTypeDescriptor,
    						skipFirstArg);
    			} else {
    				parameters = Parameters.createNode(noNodes);
    			}
    		} else {
    			MethodDeclVisitor mdv = new MethodDeclVisitor(varargs, paramTypes.length);
    			new SignatureReader(signature).accept(mdv);
    			types = TypeFormals.createNode(mdv.types);
    			// signature doesn't contain the extra arg, so no need to skip
    			parameters = createParameters(paramAnnos, mdv.paramTypes.toArray(noNodes), identity, false);
    			rType = mdv.rType;
    			exs = Throws.createNode(mdv.exTypes.toArray(noNodes));

    			if (rType == null) {
    				new SignatureReader(signature).accept(new TraceSignatureVisitor("  "));
    			}
    		}
    		final IRNode result;
			if (isConstructor) {
    			int delim = className.lastIndexOf('$');
    			if (delim < 0) {
    				delim = className.lastIndexOf('/');
    			}
    			String cname = delim < 0 ? className : className.substring(delim + 1);
    			result = ConstructorDeclaration.createNode(annos, mods, types, cname, parameters, exs, body);
    		} else {
    			String rName = desc.substring(desc.lastIndexOf(')') + 1, desc.length());
    			if (signature == null) {
    				rType = adaptTypeDescriptor(rName);
    			}
    			if (rType == null) {
    				System.out.println("null rType");
    			}
    			result = MethodDeclaration.createNode(annos, mods, types, rType, name, parameters, 0, exs, body);
    		}
			return new Triple<IRNode,IRNode,Boolean>(result, parameters, JavaNode.isSet(mods, JavaNode.STATIC));
		}

    	void addRefs(IRNode result, IRNode parameters, IRNode annos) {
    		// System.out.println("Got line#"+line);
    		if (line == Integer.MAX_VALUE) {
    			line = 0;
    		}
    		SkeletonJavaRefUtility.registerBinaryCode(declFactory, result, resource, line);
    		if (parameters != null) {
    			for (IRNode p : Parameters.getFormalIterator(parameters)) {
    				SkeletonJavaRefUtility.copyIfPossible(result, p);
    			}
    		}
    		if (annos != null) {
    			for (IRNode p : Annotations.getAnnotIterator(annos)) {
    				SkeletonJavaRefUtility.copyIfPossible(result, p);
    			}
    		}
    	}
    };
  }

  public void visitEnd() {
    if (className.endsWith(PACKAGE_INFO_CLASS)) {
    	return;
	}
    String id;
    int separator = name.lastIndexOf('/');
    int dollar = name.lastIndexOf('$');
    if (separator < dollar) {
      separator = dollar;
    }
    if (separator < 0) {
      id = name;
    } else {
      id = name.substring(separator + 1);
    }
    /*
     * if (id.equals("ThreadLocal")) { System.out.println(root); }
     */
    int mods = adaptModifiers(access);
    IRNode annos = edu.cmu.cs.fluid.java.operator.Annotations.createNode(this.annos.toArray(noNodes));
    IRNode types = TypeFormals.createNode(formals);
    IRNode body = ClassBody.createNode(members.toArray(noNodes));
    /*
     * if ("State".equals(id)) { System.out.println("Looking at "+name); }
     */

    if (isAnnotationDecl()) {
      if (isInner) {
        root = NestedAnnotationDeclaration.createNode(annos, mods, id, body);
      } else {
        root = AnnotationDeclaration.createNode(annos, mods, id, body);
      }
    } else if ((access & Opcodes.ACC_INTERFACE) != 0) {
      IRNode extensions = Extensions.createNode(ifaces);
      if (isInner) {
        root = NestedInterfaceDeclaration.createNode(annos, mods, id, types, extensions, body);
      } else {
        root = InterfaceDeclaration.createNode(annos, mods, id, types, extensions, body);
      }
    } else if (isEnumDecl()) {
      IRNode impls = Implements.createNode(ifaces);
      if (isInner) {
        root = NestedEnumDeclaration.createNode(annos, mods, id, impls, body);
      } else {
        root = EnumDeclaration.createNode(annos, mods, id, impls, body);
      }
    } else {
      IRNode ext = superType == null ? NamedType.createNode(SLUtility.JAVA_LANG_OBJECT) : superType;
      IRNode impls = Implements.createNode(ifaces);
      /*
       * if ("1".equals(id)) { System.out.println("Unusual class name: "+id); }
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
    return TypeRef.createNode(base, name.substring(dollar + 1));
  }

  public static IRNode adaptTypeDescriptor(String desc) {
    return adaptTypeDescriptor(desc, 0);
  }

  public static IRNode adaptTypeDescriptor(String desc, boolean varargs) {
    if (varargs) {
      if (desc.charAt(0) != '[') {
        throw new IllegalArgumentException("Non-array: " + desc);
      }
      IRNode base = adaptTypeDescriptor(desc, 1);
      return VarArgsType.createNode(base);
    } else {
      return adaptTypeDescriptor(desc, 0);
    }
  }

  public static IRNode adaptTypeDescriptor(String desc, int i) {
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
      return adaptTypeName(desc.substring(i + 1, semi));
    case '[':
      int j = i + 1;
      while (j < desc.length() && desc.charAt(j) == '[') {
        j++;
      }
      return ArrayType.createNode(adaptTypeDescriptor(desc, j), j - i);
    default:
      throw new IllegalArgumentException("Couldn't parse " + desc);
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

  class Visitor extends ClassVisitor {
    Visitor() {
      super(Opcodes.ASM4);
    }

    @Override
    public void visit(int version, int access, String name, String sig, String sname, String[] interfaces) {
      ClassAdapter.this.visit(version, access, name, sig, sname, interfaces);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
      return ClassAdapter.this.visitAnnotation(desc, visible);
    }

    @Override
    public void visitInnerClass(String name, String outerName, String innerName, int access) {
      ClassAdapter.this.visitInnerClass(name, outerName, innerName, access);
    }

    @Override
    public MethodVisitor visitMethod(int access, final String name, final String desc, final String signature,
        final String[] exceptions) {
      return ClassAdapter.this.visitMethod(access, name, desc, signature, exceptions);
    }

    @Override
    public FieldVisitor visitField(final int access, final String name, final String desc, final String signature, Object value) {
      return ClassAdapter.this.visitField(access, name, desc, signature, value);
    }

    @Override
    public void visitEnd() {
      ClassAdapter.this.visitEnd();
    }
  }
}
