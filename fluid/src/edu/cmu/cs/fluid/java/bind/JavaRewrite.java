package edu.cmu.cs.fluid.java.bind;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.common.SLUtility;
import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.FluidError;
import edu.cmu.cs.fluid.ide.IDE;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.*;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.promise.ReceiverDeclaration;
import edu.cmu.cs.fluid.java.promise.ReturnValueDeclaration;
import edu.cmu.cs.fluid.java.util.CogenUtil;
import edu.cmu.cs.fluid.java.util.PromiseUtil;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;

/**
 * @author Edwin Chan
 * 
 *         This contains code intended to make sure that all Java ASTs match the
 *         structure expected by analysis and binding -- e.g., explicit
 *         constructors
 */
@SuppressWarnings("deprecation")
public class JavaRewrite implements JavaGlobals {
  private static final String VALUES = "values";

  /**
   * Logger for this class
   */
  static final Logger LOG = SLLogger.getLogger("FLUID.bind");

  private final Collection<IRNode> added = new ArrayList<IRNode>();

  private void markAsAdded(IRNode parent, IRNode n) {
    added.add(n);
    JavaNode.setImplicit(n);
    IRNode decl = VisitUtil.getClosestDecl(parent);
    SkeletonJavaRefUtility.copyIfPossible(decl, n);
    /*
     * boolean found = false; for(IRNode c : JJNode.tree.children(parent)) { if
     * (c == n) { found = true; break; } } if (!found) {
     * LOG.warning("Unable to find node "+DebugUnparser.toString(n)+
     * " in parent: "+DebugUnparser.toString(parent)); }
     */
  }

  // / insertDefaultCall
  protected void insertDefaultCall(IRNode mbody, IRNode call) {
    IRNode block = MethodBody.getBlock(mbody);
    JJNode.tree.insertSubtree(block, call); // add to front
    markAsAdded(block, call);
  }

  protected void insertDefaultConstructor(IRNode cbody, IRNode constructor) {
    // JavaNode.getModifier(node, JavaNode.IMPLICIT);

    JJNode.tree.insertSubtree(cbody, constructor); // add to front
    markAsAdded(cbody, constructor);

    ReturnValueDeclaration.getReturnNode(constructor);
    PromiseUtil.addReceiverDeclsToConstructor(constructor);

    // IRNode type = VisitUtil.getEnclosingType(cbody);
    // System.out.println("Created default constructor for "+JavaNames.getFullTypeName(type));
  }

  protected boolean ensureSuperConstructorCall(IRNode constructor) {
    IRNode body = ConstructorDeclaration.getBody(constructor);
    if (!(JJNode.tree.getOperator(body) instanceof MethodBody)) {
      return false; // nothing to do
    }

    IRNode block = MethodBody.getBlock(body);

    /*
     * IRNode first = BlockStatement.getStmt(block, 0); if
     * (JJNode.tree.getOperator(first) instanceof ConstructorCall) { return; }
     */

    // HACK only needed due to old promise parsing
    Iterator<IRNode> stmts = BlockStatement.getStmtIterator(block);
    while (stmts.hasNext()) {
      IRNode stmt = stmts.next();

      if (LOG.isLoggable(Level.FINER)) {
        LOG.finer("Looking at init: " + DebugUnparser.toString(stmt));
      }
      Operator op = JJNode.tree.getOperator(stmt);
      if (ExprStatement.prototype.includes(op)) {
        IRNode e = ExprStatement.getExpr(stmt);
        Operator eop = JJNode.tree.getOperator(e);
        if (eop instanceof ConstructorCall) {
          return false; //
        } else if (eop instanceof OuterObjectSpecifier && ConstructorCall.prototype.includes(OuterObjectSpecifier.getCall(e))) {
          return false;
        }
      }
    }
    IRNode type = VisitUtil.getEnclosingType(constructor);
    IRNode call;
    if (EnumDeclaration.prototype.includes(type)) {
      call = makeEnumSuperConstructorCall();
    } else {
      String name = JavaNames.getQualifiedTypeName(type);
      if (SLUtility.JAVA_LANG_OBJECT.equals(name)) {
        return false;
      }
      call = CogenUtil.makeDefaultSuperCall();
      // System.out.println("Creating super(): "+call);
    }
    JavaNode.setModifiers(call, JavaNode.IMPLICIT);
    insertDefaultCall(body, call);
    return true;
  }

  private static IRNode makeEnumSuperConstructorCall() {
    // Need a call to Enum(String, int)
    IRNode[] args = new IRNode[2];
    args[0] = StringLiteral.createNode("");
    args[1] = IntLiteral.createNode("0");
    IRNode call = NonPolymorphicConstructorCall.createNode(SuperExpression.prototype.jjtCreate(), Arguments.createNode(args));
    call = ExprStatement.createNode(call);
    return call;
  }

  // Could be null
  protected final ITypeEnvironment te;

  public JavaRewrite(ITypeEnvironment te) {
    this.te = te;
  }

  /**
   * Applies defaults to each type declared in the compilation unit
   * 
   * @param cu
   * @return true if changed
   */
  public boolean ensureDefaultsExist(IRNode cu) {
    final boolean debug = LOG.isLoggable(Level.FINER);
    boolean changed = false;

    IRNode decls = CompilationUnit.getDecls(cu);
    Iterator<IRNode> enm = JJNode.tree.children(decls);

    while (enm.hasNext()) {
      IRNode x = enm.next();
      Operator op = JJNode.tree.getOperator(x);

      if (ClassDeclaration.prototype.includes(op) || EnumDeclaration.prototype.includes(op)) {
        String qname = JavaNames.getFullTypeName(x);

        // not for interfaces
        if (debug) {
          LOG.finer("Ensuring constructor for " + qname);
        }
        if (qname.equals(SLUtility.JAVA_LANG_OBJECT)) {
          changed |= ensureConstructorStuffForObject(x);
        } else {
          changed |= ensureConstructorStuff(x);
          if (EnumDeclaration.prototype.includes(op)) {
            if (!JavaNode.getModifier(x, JavaNode.AS_BINARY) || missingValuesMethod(x)) {
              // System.err.println("Adding implicit methods for "+qname+" = "+x);
              addImplicitEnumMethods(x);
              changed = true;
            } else {
              // System.err.println("Not adding implicit methods to "+qname);
            }
          }
        }
        // System.out.println("Ensuring defaults for "+qname);
        changed |= ensureDefaultsExistForType(x);
      } else if (InterfaceDeclaration.prototype.includes(op)) {
        changed |= ensureDefaultsExistForType(x);
      } else if (AnnotationDeclaration.prototype.includes(op)) {
        // System.err.println("Ignoring AnnotationDecl: " + JJNode.getInfo(x));
        changed |= ensureDefaultsExistForType(x);
      } else {
        LOG.severe("Ignoring " + op.name() + ": " + JJNode.getInfo(x));
      }
    }

    changed |= addSrcRefs();
    return changed;
  }

  private boolean addSrcRefs() {
    final boolean fineIsLoggable = LOG.isLoggable(Level.FINE);

    // Post-process nodes to generate a SrcRef?
    for (IRNode n : added) {
      Operator op = jtree.getOperator(n);

      // Assuming inserted at the front of any siblings
      if (op instanceof ConstructorDeclaration || op instanceof ExprStatement)// ConstructorCall)
      {
        // System.out.println("Looking at "+DebugUnparser.toString(n));
        final IRNode p = jtree.getParentOrNull(n);
        final int ni;
        try {
          ni = jtree.childLocationIndex(p, jtree.getLocation(n));
        } catch (IndexOutOfBoundsException e) {
          LOG.log(Level.WARNING, "Unable to find node " + DebugUnparser.toString(n) + " in parent: " + DebugUnparser.toString(p));
          continue;
        }
        assert (ni == 0);

        IRNode contextRef = null;
        if (contextRef == null) {
          // try next sibling
          int sib = ni + 1;
          if (sib < jtree.numChildren(p)) {
        	  final IRNode sibling = jtree.getChild(p, sib);
        	  if (SkeletonJavaRefUtility.hasRegistered(sibling)) {
        		  contextRef = sibling;
        	  }
        	  if (LOG.isLoggable(Level.FINE))
        		  LOG.fine("Created src ref from next sibling: " + op);
          }
        }
        IRNode ancestor = p;
        for (int i = 1; ancestor != null; i++) {
        	// try ancestors
        	if (SkeletonJavaRefUtility.hasRegistered(ancestor)) {
        		contextRef = ancestor;
        		break;
        	}
        	ancestor = jtree.getParentOrNull(ancestor);

        	if (contextRef != null) {
        		if (LOG.isLoggable(Level.FINE))
        			LOG.fine("Created src ref from ancestors " + i + " gens up: " + op);
        	}
        }

        if (contextRef == null) {
          // Couldn't find an ancestor
          IRNode context = null;
          if (op instanceof ExprStatement) {// ConstructorCall) {
            context = VisitUtil.getEnclosingClassBodyDecl(n);
          } else {
            // usually because the new constructor decl is from a
            // type binding
            context = VisitUtil.getEnclosingType(n);
          }
          if (fineIsLoggable) {
            LOG.fine("Could not find a src ref to make the dummy for " + op + " out of: " + DebugUnparser.toString(context));
          }
          continue; // skip
        }
        copyRefToSubtree(contextRef, n);
      } else if (op instanceof MethodDeclaration) {
        IRNode p = VisitUtil.getEnclosingDecl(n);//jtree.getParentOrNull(n);
        SkeletonJavaRefUtility.copyIfPossible(p, n);
      } else {
        LOG.severe("Unexpected AST nodes: " + op);
      }
    }
    final boolean changed = !added.isEmpty();
    added.clear();
    return changed;
  }

  private void copyRefToSubtree(final IRNode src, final IRNode targetRoot) {
    Iterator<IRNode> enm = jtree.topDown(targetRoot);
    while (enm.hasNext()) {
      IRNode node = enm.next();
      SkeletonJavaRefUtility.copyIfPossible(src, node);
    }
  }

  /**
   * Iterates over the AST to apply defaults to all type declarations
   */
  private boolean ensureDefaultsExistForType(IRNode type) {
    final boolean debug = LOG.isLoggable(Level.FINER);
    boolean changed = false;

    Iterator<IRNode> enm = VisitUtil.getAllTypeDecls(type);
    while (enm.hasNext()) {
      IRNode y = enm.next();
      if (y == type) {
        continue;
      }
      Operator op = JJNode.tree.getOperator(y);
      if (ClassDeclaration.prototype.includes(op)) {
        // not for interfaces
        String qname = JavaNames.getFullTypeName(y);

        if (debug) {
          LOG.finer("Ensuring constructor for " + qname);
        }
        changed |= ensureConstructorStuff(y);
      } else if (InterfaceDeclaration.prototype.includes(op)) {
        // nothing to do
      } else if (AnonClassExpression.prototype.includes(op)) {
        // LOG.warning("No constructor created for anon class");
        /*
         * JJNode.tree.insertSubtree(AnonClassExpression.getBody(y),
         * makeAnonConstructor(y));
         */
        // markAsAdded(?)
      } else if (EnumDeclaration.prototype.includes(op)) {
        ensureConstructorStuff(y);
        if (debug)
          LOG.finer("Adding implicit methods for " + JavaNames.getTypeName(type) + "." + JavaNames.getTypeName(y));
        // System.out.println("Adding implicit enum methods for "+JavaNames.getFullTypeName(y));
        addImplicitEnumMethods(y);
        changed = true;
      } else if (TypeFormal.prototype.includes(op)) {
        // nothing to do
      } else if (EnumConstantClassDeclaration.prototype.includes(op)) {
        // nothing to do?
      } else if (NestedAnnotationDeclaration.prototype.includes(op)) {
        // nothing to do?
      } else {
        LOG.severe("Ignoring nested " + op.name() + ": " + JJNode.getInfo(y));
      }
    }
    return changed;
  }

  private boolean missingValuesMethod(IRNode t) {
    for (IRNode m : VisitUtil.getClassMethods(t)) {
      if (!MethodDeclaration.prototype.includes(m)) {
        continue;
      }
      if (VALUES.equals(JJNode.getInfoOrNull(m))) {
        final IRNode params = MethodDeclaration.getParams(m);
        if (JJNode.tree.numChildren(params) == 0) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * Add static E[] values() and static E valueOf(String n)
   * 
   * @param ed
   *          The enum declaration
   */
  private void addImplicitEnumMethods(IRNode ed) {
    final IRNode values = makeValuesMethod(ed);
    final IRNode valueOf = makeValueOfMethod(ed);
    /*
     * final IRNode name = makeNameMethod(ed);
     */
    final IRNode body = EnumDeclaration.getBody(ed);
    JJNode.tree.appendSubtree(body, values);
    JJNode.tree.appendSubtree(body, valueOf);

    markAsAdded(body, values);
    markAsAdded(body, valueOf);
    /*
     * JJNode.tree.appendSubtree(body, name);
     */
  }

  private IRNode makeTypeName(final IRNode decl) {
    IRNode enclosingT = VisitUtil.getEnclosingType(decl);
    if (enclosingT == null) { // outermost type
      String name = JavaNames.getFullTypeName(decl);
      return NamedType.createNode(name);
    }
    // An inner class
    IRNode base = makeTypeName(enclosingT);
    return TypeRef.createNode(base, JJNode.getInfo(decl));
  }

  private IRNode makeValuesMethod(final IRNode ed) {
    int mods = JavaNode.PUBLIC | JavaNode.STATIC | JavaNode.IMPLICIT;
    IRNode type = ArrayType.createNode(makeTypeName(ed), 1);
    IRNode body = OmittedMethodBody.prototype.jjtCreate();
    IRNode rv = CogenUtil.makeMethodDecl(noNodes, mods, noNodes, type, VALUES, noNodes, noNodes, body);
    ReturnValueDeclaration.makeReturnNode(rv);
    return rv;
  }

  private IRNode makeValueOfMethod(final IRNode ed) {
    int mods = JavaNode.PUBLIC | JavaNode.STATIC | JavaNode.IMPLICIT;
    IRNode type = makeTypeName(ed);
    IRNode[] params = new IRNode[] { CogenUtil.makeParamDecl("name", NamedType.createNode("java.lang.String")) };
    IRNode body = OmittedMethodBody.prototype.jjtCreate();
    IRNode rv = CogenUtil.makeMethodDecl(noNodes, mods, noNodes, type, "valueOf", params, noNodes, body);
    ReturnValueDeclaration.makeReturnNode(rv);
    return rv;
  }

  /*
   * private IRNode makeNameMethod(final IRNode ed) { int mods =
   * JavaNode.PUBLIC; IRNode type = NamedType.createNode("java.lang.String");
   * IRNode body = OmittedMethodBody.prototype.jjtCreate(); IRNode rv =
   * CogenUtil.makeMethodDecl(noNodes, mods, noNodes, type, "name", noNodes,
   * noNodes, body); markAsAdded(rv); return rv; }
   */

  // protected IRNode makeAnonConstructor(IRNode anonE, IRNode stype) {
  // int mods = JavaNode.PUBLIC;
  // String cName = "<anon>";
  // // TODO: Should copy the arguments in the called signature
  // // ... rather than try to create a lot of new stuff here
  // IJavaType[] params =
  // te.getBinder().copyCallSig(AnonClassExpression.getArgs(anonE));
  // IRNode sConstructor = findConstructor(stype, params);
  //
  // // copy parent's throws clause
  // IRNode[] throwsC = copyConstructorThrows(sConstructor);
  //
  // // create a param decl for each type created by copyCallSig
  // for (int i = 0; i < params.length; i++) {
  // params[i] =
  // ParameterDeclaration.createNode(
  // JavaNode.ALL_FALSE,
  // null, // No type? That seems wrong, see above (TODO)
  // "param" + i);
  // }
  // IRNode body = NoMethodBody.prototype.jjtCreate();
  // return CogenUtil.makeConstructorDecl(mods, cName, params, throwsC, body);
  // }

  /**
   * @param stype
   * @param params
   * @return
   * 
   *         private IRNode findConstructor(IRNode stype, IRNode[] params) { //
   *         TODO Auto-generated method stub return null; }
   */

  private boolean ensureConstructorStuffForObject(IRNode type) {
    IRNode cbody = VisitUtil.getClassBody(type);

    // Find out if there's a constructor
    Iterator<IRNode> enm = JJNode.tree.children(cbody);
    while (enm.hasNext()) {
      IRNode n = enm.next();
      Operator op = JJNode.tree.getOperator(n);

      if (op == ConstructorDeclaration.prototype) {
        return false; // Already a constructor
      }
    }
    // Add a default constructor since there isn't one
    IRNode dc = makeEmptyConstructor();
    insertDefaultConstructor(cbody, dc);

    if (LOG.isLoggable(Level.FINER)) {
      LOG.finer("Adding default constructor to " + JJNode.getInfo(type));
    }
    return true;
  }

  // / ensureConstructorStuff
  /**
   * Create previously-implicit calls to superclass constructors, as well as
   * default constructors
   * 
   * @return true if changed
   */
  public boolean ensureConstructorStuff(IRNode type) {
    // System.out.println("Looking for constructor in "+JavaNames.getFullTypeName(type));
    IRNode cbody = VisitUtil.getClassBody(type);
    boolean changed = false;

    // Add a default super() if constructor don't have it
    // based on findDefaultConstructor
    boolean someCon = false;
    Iterator<IRNode> enm = JJNode.tree.children(cbody);
    while (enm.hasNext()) {
      IRNode n = enm.next();
      Operator op = JJNode.tree.getOperator(n);

      if (op == ConstructorDeclaration.prototype) {
        someCon = true;
        changed |= ensureSuperConstructorCall(n); // add super() if needed
      }
    }

    // Add a default constructor if there aren't any
    if (!someCon) {
      final IRNode dc;
      if (EnumDeclaration.prototype.includes(type)) {
        dc = makeDefaultEnumConstructor(type);
      } else {
        dc = makeDefaultConstructor(type);
      }
      insertDefaultConstructor(cbody, dc);

      if (LOG.isLoggable(Level.FINER)) {
        String qname = JavaNames.getFullTypeName(type);
        LOG.finer("Adding default constructor to " + qname);
        /*
         * if (qname.endsWith("TestMultipleTextOutputFormat") ||
         * qname.endsWith("TestMultiFileInputFormat")) {
         * System.out.println(DebugUnparser
         * .childrenToString(VisitUtil.getClassBody(type))); }
         */
      }
      changed = true;
    }
    return changed;
  }

  // / findDefaultConstructor -- based on BSI's findConstructor
  public static IRNode findDefaultConstructor(IRNode cbody) {
    Iterator<IRNode> enm = JJNode.tree.children(cbody);
    while (enm.hasNext()) {
      IRNode n = enm.next();
      Operator op = JJNode.tree.getOperator(n);

      // LOG.info("Looking at "+DebugUnparser.toString(n));
      if (op == ConstructorDeclaration.prototype) {
        IRNode params = ConstructorDeclaration.getParams(n);
        if (JJNode.tree.numChildren(params) == 0) {
          return n;
        }
      }
    }
    return null;
  }

  protected boolean isSuper_JavaLangObject(IRNode decl, IJavaDeclaredType type) {
    if (te == null) {
      return false;
    }

    Operator op = JJNode.tree.getOperator(decl);
    /*
     * return (op instanceof TypeDeclInterface) &&
     * (te.getBinder().getSuperclass(type) == null);
     */
    if (op instanceof TypeDeclInterface) {
      IRNode nt;
      if (ClassDeclaration.prototype.includes(op)) {
        nt = ClassDeclaration.getExtension(decl);
        Operator eop = JJNode.tree.getOperator(nt);

        if (ParameterizedType.prototype.includes(eop)) {
          return false;
        }
        if (TypeRef.prototype.includes(eop)) {
          return false;
        }
        String name;
        if (NameType.prototype.includes(eop)) {
          name = DebugUnparser.toString(nt);
        } else {
          name = NamedType.getType(nt);
        }
        if (SLUtility.JAVA_LANG_OBJECT.equals(name)) {
          return true;
        }
        if (!name.endsWith("Object")) {
          return false;
        }
      } else if (InterfaceDeclaration.prototype.includes(op)) {
        return true;
      }
      return te.getBinder().getSuperclass(type) == null;
    }
    return false;
  }

  /**
   * For java.lang.Object
   */
  private IRNode makeEmptyConstructor() {
    int mods = JavaNode.PUBLIC | JavaNode.IMPLICIT;
    String cName = "Object";
    IRNode[] params = noNodes;
    IRNode[] stmt = noNodes;
    IRNode[] throwsC = noNodes;

    IRNode block = BlockStatement.createNode(stmt);
    IRNode body = MethodBody.createNode(block);
    return CogenUtil.makeConstructorDecl(noNodes, mods, noNodes, cName, params, throwsC, body);
  }

  // / makeDefaultConstructor
  IRNode makeDefaultConstructor(IRNode decl) {
    int mods = JavaNode.PUBLIC | JavaNode.IMPLICIT;
    String cName = JJNode.getInfo(decl);
    IRNode[] params = noNodes;

    // copy parent's throws clause
    IRNode[] throwsC;
    IJavaDeclaredType type = (IJavaDeclaredType) JavaTypeFactory.getMyThisType(decl);
    if (!isSuper_JavaLangObject(decl, type)) {
      throwsC = makeDefaultThrows(type);
    } else {
      throwsC = JavaGlobals.noNodes;
    }
    final boolean isJavaLangObject;
    if ("Object".equals(cName)) {
      final String qname = JavaNames.getQualifiedTypeName(decl);
      isJavaLangObject = SLUtility.JAVA_LANG_OBJECT.equals(qname);
    } else {
      isJavaLangObject = false;
    }
    IRNode[] stmt = isJavaLangObject ? noNodes : new IRNode[] { CogenUtil.makeDefaultSuperCall() };
    // System.out.println("Creating constructor with super(): "+stmt[0]);
    IRNode block = BlockStatement.createNode(stmt);
    IRNode body = MethodBody.createNode(block);
    IRNode constructor = CogenUtil.makeConstructorDecl(noNodes, mods, noNodes, cName, params, throwsC, body);
    ReceiverDeclaration.makeReceiverNode(constructor);
    return constructor;
  }

  IRNode makeDefaultEnumConstructor(IRNode decl) {
    int mods = JavaNode.PRIVATE | JavaNode.IMPLICIT;
    String cName = JJNode.getInfo(decl);
    IRNode[] stmt = new IRNode[] { makeEnumSuperConstructorCall() };
    IRNode block = BlockStatement.createNode(stmt);
    IRNode body = MethodBody.createNode(block);
    IRNode constructor = CogenUtil.makeConstructorDecl(noNodes, mods, noNodes, cName, noNodes, noNodes, body);
    ReceiverDeclaration.makeReceiverNode(constructor);
    return constructor;
  }

  IRNode[] makeDefaultThrows(IJavaDeclaredType type) {
	if (te == null) {
	  return null; 
	}
	  
    // assuming n is the enclosing type, find super's default constructor
    IJavaDeclaredType stype = te.getBinder().getSuperclass(type);
    if (stype != null) {
      // type != java.lang.Object
      IRNode sdecl = stype.getDeclaration();
      IRNode body = VisitUtil.getClassBody(sdecl);
      IRNode dc = findDefaultConstructor(body);
      if (dc == null) {
        if (LOG.isLoggable(Level.FINER))
          LOG.finer("Couldn't find a default constructor in " + stype + " - making one");

        dc = makeDefaultConstructor(sdecl);
        insertDefaultConstructor(body, dc);

        IDE.getInstance().notifyASTChanged(VisitUtil.getEnclosingCompilationUnit(sdecl));
      }
      return copyConstructorThrows(dc);
    }
    return JavaGlobals.noNodes;
  }

  IRNode[] copyConstructorThrows(IRNode constructor) {
    // copy its throws clause, resolving its type names(?)
    IRNode throws0 = ConstructorDeclaration.getExceptions(constructor);
    Iterator<IRNode> enm = JJNode.tree.children(throws0);
    if (!enm.hasNext()) {
      return JavaGlobals.noNodes;
    }
    Vector<IRNode> throws1 = new Vector<IRNode>();

    do {
      IRNode n = enm.next();
      throws1.addElement(copyTypeNodes(n));
    } while (enm.hasNext());

    return CogenUtil.makeNodeArray(throws1);
  }

  // / copyTypeNodes (using its binding)
  IRNode copyTypeNodes(IRNode x) {
	if (te == null) {
	  throw new IllegalStateException("Should never be called with null type env");
	}
	
    JavaOperator op = (JavaOperator) JJNode.tree.getOperator(x);
    if (op instanceof NamedType || op instanceof NameType) {
      // Need to bind first, because this will be in a different CU
      IRNode type = te.getBinder().getBinding(x);
      if (type == null) {
    	  // Resort to using the name directly
    	  return NamedType.createNode(JJNode.getInfo(x));
      }
      return buildNamedType(type);
    } else if (op instanceof ArrayType) {
      IRNode base = ArrayType.getBase(x);
      int dims = ArrayType.getDims(x);
      return ArrayType.createNode(copyTypeNodes(base), dims);
    } else if (op instanceof PrimitiveType) {
      return op.jjtCreate();
    } else
      throw new FluidError("Got unknown type node : " + op);
  }

  IRNode buildNamedType(IRNode tdecl) {
    IRNode enclosingT = VisitUtil.getEnclosingType(tdecl);
    if (enclosingT == null) {
      String name = JavaNames.getFullTypeName(tdecl);
      return NamedType.createNode(name);
    }
    IRNode base = buildNamedType(enclosingT);
    return TypeRef.createNode(base, JavaNames.getTypeName(tdecl));
  }
}
