/*
 * $header$
 * Created on Jul 12, 2005
 */
package edu.cmu.cs.fluid.java.bind;

import static edu.cmu.cs.fluid.java.JavaGlobals.noNodes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.RegionLock;
import com.surelogic.ThreadSafe;
import com.surelogic.common.AnnotationConstants;
import com.surelogic.common.SLUtility;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.javac.Projects;

import edu.cmu.cs.fluid.FluidError;
import edu.cmu.cs.fluid.ir.IRLocation;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.IRRegion;
import edu.cmu.cs.fluid.CommonStrings;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.JavaOperator;
import edu.cmu.cs.fluid.java.SkeletonJavaRefUtility;
import edu.cmu.cs.fluid.java.adapter.AbstractAdapter;
import edu.cmu.cs.fluid.java.operator.AddExpression;
import edu.cmu.cs.fluid.java.operator.AnnotationDeclaration;
import edu.cmu.cs.fluid.java.operator.AnnotationElement;
import edu.cmu.cs.fluid.java.operator.Annotations;
import edu.cmu.cs.fluid.java.operator.AnonClassExpression;
import edu.cmu.cs.fluid.java.operator.Arguments;
import edu.cmu.cs.fluid.java.operator.ArrayCreationExpression;
import edu.cmu.cs.fluid.java.operator.ArrayInitializer;
import edu.cmu.cs.fluid.java.operator.ArrayLength;
import edu.cmu.cs.fluid.java.operator.ArrayRefExpression;
import edu.cmu.cs.fluid.java.operator.ArrayType;
import edu.cmu.cs.fluid.java.operator.AssignExpression;
import edu.cmu.cs.fluid.java.operator.BlockStatement;
import edu.cmu.cs.fluid.java.operator.BoxExpression;
import edu.cmu.cs.fluid.java.operator.BoxingOpAssignExpression;
import edu.cmu.cs.fluid.java.operator.CastExpression;
import edu.cmu.cs.fluid.java.operator.ClassBody;
import edu.cmu.cs.fluid.java.operator.ClassDeclaration;
import edu.cmu.cs.fluid.java.operator.ClassInitializer;
import edu.cmu.cs.fluid.java.operator.ConditionalExpression;
import edu.cmu.cs.fluid.java.operator.ConstructorCall;
import edu.cmu.cs.fluid.java.operator.ConstructorDeclaration;
import edu.cmu.cs.fluid.java.operator.ConstructorReference;
import edu.cmu.cs.fluid.java.operator.DeclStatement;
import edu.cmu.cs.fluid.java.operator.ElementValuePair;
import edu.cmu.cs.fluid.java.operator.EnumConstantClassDeclaration;
import edu.cmu.cs.fluid.java.operator.EnumConstantDeclaration;
import edu.cmu.cs.fluid.java.operator.ExprStatement;
import edu.cmu.cs.fluid.java.operator.Expression;
import edu.cmu.cs.fluid.java.operator.FieldDeclaration;
import edu.cmu.cs.fluid.java.operator.FieldRef;
import edu.cmu.cs.fluid.java.operator.ForEachStatement;
import edu.cmu.cs.fluid.java.operator.ForStatement;
import edu.cmu.cs.fluid.java.operator.IllegalCode;
import edu.cmu.cs.fluid.java.operator.Initialization;
import edu.cmu.cs.fluid.java.operator.IntLiteral;
import edu.cmu.cs.fluid.java.operator.IntType;
import edu.cmu.cs.fluid.java.operator.LambdaExpression;
import edu.cmu.cs.fluid.java.operator.LessThanExpression;
import edu.cmu.cs.fluid.java.operator.MethodBody;
import edu.cmu.cs.fluid.java.operator.MethodDeclaration;
import edu.cmu.cs.fluid.java.operator.MethodReference;
import edu.cmu.cs.fluid.java.operator.NameExpression;
import edu.cmu.cs.fluid.java.operator.NameType;
import edu.cmu.cs.fluid.java.operator.NamedType;
import edu.cmu.cs.fluid.java.operator.NestedAnnotationDeclaration;
import edu.cmu.cs.fluid.java.operator.NestedClassDeclaration;
import edu.cmu.cs.fluid.java.operator.NestedDeclInterface;
import edu.cmu.cs.fluid.java.operator.NestedEnumDeclaration;
import edu.cmu.cs.fluid.java.operator.NestedTypeDeclInterface;
import edu.cmu.cs.fluid.java.operator.NewExpression;
import edu.cmu.cs.fluid.java.operator.NonPolymorphicMethodCall;
import edu.cmu.cs.fluid.java.operator.NonPolymorphicNewExpression;
import edu.cmu.cs.fluid.java.operator.OpAssignExpression;
import edu.cmu.cs.fluid.java.operator.OuterObjectSpecifier;
import edu.cmu.cs.fluid.java.operator.ParameterDeclaration;
import edu.cmu.cs.fluid.java.operator.ParameterizedType;
import edu.cmu.cs.fluid.java.operator.Parameters;
import edu.cmu.cs.fluid.java.operator.PrimitiveType;
import edu.cmu.cs.fluid.java.operator.QualifiedName;
import edu.cmu.cs.fluid.java.operator.QualifiedThisExpression;
import edu.cmu.cs.fluid.java.operator.ReturnStatement;
import edu.cmu.cs.fluid.java.operator.SimpleName;
import edu.cmu.cs.fluid.java.operator.SingleElementAnnotation;
import edu.cmu.cs.fluid.java.operator.SomeFunctionDeclaration;
import edu.cmu.cs.fluid.java.operator.StringConcat;
import edu.cmu.cs.fluid.java.operator.StringLiteral;
import edu.cmu.cs.fluid.java.operator.ThisExpression;
import edu.cmu.cs.fluid.java.operator.Throws;
import edu.cmu.cs.fluid.java.operator.Type;
import edu.cmu.cs.fluid.java.operator.TypeActuals;
import edu.cmu.cs.fluid.java.operator.TypeDeclInterface;
import edu.cmu.cs.fluid.java.operator.TypeDeclaration;
import edu.cmu.cs.fluid.java.operator.TypeExpression;
import edu.cmu.cs.fluid.java.operator.TypeFormals;
import edu.cmu.cs.fluid.java.operator.TypeRef;
import edu.cmu.cs.fluid.java.operator.UnboxExpression;
import edu.cmu.cs.fluid.java.operator.VarArgsExpression;
import edu.cmu.cs.fluid.java.operator.VarArgsType;
import edu.cmu.cs.fluid.java.operator.VariableDeclaration;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.operator.VariableDeclarators;
import edu.cmu.cs.fluid.java.operator.VariableUseExpression;
import edu.cmu.cs.fluid.java.operator.Visitor;
import edu.cmu.cs.fluid.java.operator.VoidType;
import edu.cmu.cs.fluid.java.operator.WildcardExtendsType;
import edu.cmu.cs.fluid.java.operator.WildcardSuperType;
import edu.cmu.cs.fluid.java.operator.WildcardType;
import edu.cmu.cs.fluid.java.promise.ReceiverDeclaration;
import edu.cmu.cs.fluid.java.promise.ReturnValueDeclaration;
import edu.cmu.cs.fluid.java.util.CogenUtil;
import edu.cmu.cs.fluid.java.util.PromiseUtil;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.tree.SyntaxTree;
import edu.cmu.cs.fluid.version.Era;
import edu.cmu.cs.fluid.version.Version;
import edu.cmu.cs.fluid.version.VersionedRegionDelta;

/**
 * Convert a FAST that was parsed in from external sources into the canonical
 * form so it is ready for analysis. We use the binder to determine how to
 * canonicalize the nodes. XXX: we need to generate boxing around string
 * concatenation arguments.
 * 
 * @see edu.cmu.cs.fluid.java.parse.JavaParser
 * @author boyland
 */
@ThreadSafe
@RegionLock("StatusLock is class protects isCanonicalizing")
public class JavaCanonicalizer {
  private static final Logger LOG = SLLogger.getLogger("FLUID.java.bind");

  private static final IRNode[] none = JavaGlobals.noNodes;

  private final IBinder binder;

  private final IBinderCache bindCache;

  private final ITypeEnvironment tEnv;

  private final DoCanon doWork = new DoCanon();

  private final JavaRewrite rewrite;
  
  private final SyntaxTree tree = (SyntaxTree) JJNode.tree; // NB: must be
                                                            // mutable!

  // For debugging
  private static boolean isCanonicalizing = false;

  private enum TypeParamHandling {
  	KEEP, KEEP_IF_NOT_ALL_FORMALS, DISCARD
  }
  
  private static synchronized void setActive(boolean state) {
    isCanonicalizing = state;
  }

  public static synchronized boolean isActive() {
    return isCanonicalizing;
  }

  /**
   * Create a canonicalizer that converts an AST from the raw parsed version to
   * the canonical internal representation. It uses the bindings in the version
   * that is current at the time this constructor is called to perform AST
   * canonicalization.
   * 
   * @param b
   *          binder for the current version.
   */
  public JavaCanonicalizer(IBinder b) {
    binder = fixBinder(b);
    if (binder instanceof IBinderCache) {
      bindCache = (IBinderCache) binder;
    } else {
      bindCache = null;
    }
    tEnv = binder.getTypeEnvironment();
    rewrite = new JavaRewrite(tEnv);
  }

  protected IBinder fixBinder(IBinder b) {
    if (!JJNode.versioningIsOn) {
      return CachingBinder.create(b);
    }
    return VersionFixedBinder.fix(b);
  }

  interface IBinderCache {
    void init(IRNode tree);

    IBinding checkForBinding(IRNode node);

    IJavaType checkForType(IRNode node);

    void map(IRNode old, IRNode now);

    void addBinding(IRNode node, IBinding b);

    void finish(IRNode tree);
  }

  private void init(IRNode tree) {
    setActive(true);
    if (bindCache != null) {
      bindCache.init(tree);
    }
  }

  private void map(IRNode old, IRNode now) {
    if (old.equals(now)) {
      return;
    }
    if (bindCache != null) {
      bindCache.map(old, now);
    }
  }

  private void addBinding(IRNode n, IBinding b) {
    if (bindCache != null) {
      bindCache.addBinding(n, b);
    }
  }

  private void finish(IRNode tree) {
    if (bindCache != null) {
      bindCache.finish(tree);
    }
    setActive(false);
  }

  /**
   * Fix a tree that has been parsed in and bound so that it uses canonical
   * operators. The bindings from the version that was current at the time this
   * canonicalizer was created will be used. As a side-effect, the version is
   * changed so that the current version of the tree is canonical.
   * 
   * @param tree
   *          a (probably non-canonical) Java AST node.
   * @return true if changed
   */
  public boolean canonicalize(IRNode tree) {
    init(tree);
    try {
      return doWork.doAccept(tree);
    } finally {
      finish(tree);
    }
  }

  protected void checkNode(IRNode node) {
    if (!JJNode.versioningIsOn) {
      return;
    }
    IRRegion reg = IRRegion.getOwner(node);
    if (reg instanceof VersionedRegionDelta) {
      Era e = ((VersionedRegionDelta) reg).getEra();
      Version v = ((VersionFixedBinder) binder).getAtVersion();
      if (e.getRoot().comesFrom(v)) {
        // LOG.severe("visiting nodes that were created during canonicalization!");
        throw new IllegalStateException("re-canon not allowed");
      }
    }
  }

  protected void replaceSubtree(IRNode orig, IRNode sub) {
    tree.replaceSubtree(orig, sub);
    SkeletonJavaRefUtility.copyIfPossible(orig, sub);
  }

  // To avoid binding problems, we can do analysis pre-order (before changes)
  // but perform the changes themselves post-order.
  //
  // Returns true if anything changed
  @ThreadSafe
  class DoCanon extends Visitor<Boolean> {
    @Override
    public Boolean doAccept(IRNode node) {
      checkNode(node);
      return super.doAccept(node);
    }

    /**
     * doAcceptForChildren(), except in reverse order
     */
    public Boolean doAcceptForChildren_rev(IRNode node) {
      return doAcceptForChildren_rev(node, null);
    }

    public Boolean doAcceptForChildren_rev(IRNode node, IRNode skip) {
      int i = JJNode.tree.numChildren(node) - 1;
      boolean changed = false;
      while (i >= 0) {
        IRNode c = JJNode.tree.getChild(node, i);
        if (!c.equals(skip)) {
          changed |= doAccept(c);
        }
        i--;
      }
      return changed;
    }

    @Override
    public Boolean visit(IRNode node) {
      Operator operator = tree.getOperator(node);
      if (operator instanceof IllegalCode) {
        LOG.severe("Canonicalizer didn't handle illegal operator: " + operator);
      }
      return doAcceptForChildren_rev(node);
    }

    protected boolean generateBoxUnbox(IRNode node) {
      Operator op = tree.getOperator(node);
      if (op instanceof Expression) {
        IJavaType t = binder.getJavaType(node);
        if (isPrimitive(node, t)) { // / XXX: Bug!: this is requested on new
                                    // nodes, can crash versioned fixed binder
          if (contextIsReference(node)) {
            boxExpression(node);
            return true;
          }
        } else if (couldBeUnboxed(t)) {
          if (contextIsPrimitive(node)) {
            unboxExpression(node);
            return true;
          }
        }
      }
      return false;
    }

    /**
     * @param node
     */
    private void unboxExpression(IRNode node) {
      if (LOG.isLoggable(Level.FINE)) {
        LOG.fine("Adding unboxing around " + DebugUnparser.toString(node));
      }
      IRNode newUnbox = JavaNode.makeJavaNode(UnboxExpression.prototype);
      replaceSubtree(node, newUnbox);
      UnboxExpression.setOp(newUnbox, node);
    }

    /**
     * Force the given expression to be boxed, regardless of its type.
     * 
     * @param node
     */
    private void boxExpression(IRNode node) {
      if (LOG.isLoggable(Level.FINE)) {
        LOG.fine("Adding boxing around " + DebugUnparser.toString(node));
      }
      /*
       * String unparse =
       * DebugUnparser.toString(JJNode.tree.getParentOrNull(node)); if
       * (unparse.contains("2, 3, 4")) { System.out.println("Boxing 1");
       * contextIsReference(node); }
       */
      IRNode newBox = JavaNode.makeJavaNode(BoxExpression.prototype);
      replaceSubtree(node, newBox);
      BoxExpression.setOp(newBox, node);
    }

    /**
     * Check if it's one of the possible declared types
     */
    private boolean couldBeUnboxed(IJavaType t) {
      if (t instanceof IJavaDeclaredType) {
        IJavaDeclaredType dt = (IJavaDeclaredType) t;
        // System.out.println(dt.getName());
        return JavaTypeFactory.hasCorrespondingPrimType(dt);
      }
      return false;
    }

    protected boolean generateToString(IRNode node, IJavaType type) {
      if (type == tEnv.getStringType()) {
        return false;
      }
      if (type == JavaTypeFactory.nullType) {
        // Replace with String
        IRNode nullString = StringLiteral.createNode("\"null\"");
        JavaNode.setImplicit(nullString);
        replaceSubtree(node, nullString);
        return true;
      }
      /*
       * if (type instanceof IJavaPrimitiveType) {
       * generateConversionToString(node, (IJavaPrimitiveType) type); return; }
       */
      // Constructed this way to preserve the location of 'node'
      // BUG! We need to avoid calling toSTring if receiver is null
      // (dynamically).
      LOG.fine("Adding toString around " + DebugUnparser.toString(node));
      IRNode newArgs = Arguments.createNode(noNodes);
      JavaNode.setImplicit(newArgs);
      IRNode mc = JavaNode.makeJavaNode(NonPolymorphicMethodCall.prototype);
      replaceSubtree(node, mc);
      finishToString(mc, node, newArgs);
      return true;
    }

//    private void generateConversionToString(IRNode node, IJavaPrimitiveType type) {
//      String typeName = type.getCorrespondingTypeName();
//      if (typeName == null) {
//        throw new IllegalArgumentException("No type for " + type);
//      }
//      // Constructed this way to preserve the location of 'node'
//      IRNode nt = NamedType.createNode(typeName);
//      IRNode te = TypeExpression.createNode(nt);
//      JavaNode.setImplicit(te);
//      IRNode mc = JavaNode.makeJavaNode(NonPolymorphicMethodCall.prototype);
//      replaceSubtree(node, mc);
//
//      IRNode args = Arguments.createNode(new IRNode[] { node });
//      finishToString(mc, te, args);
//    }

    private void finishToString(IRNode mc, IRNode base, IRNode args) {
      NonPolymorphicMethodCall.setObject(mc, base);
      JJNode.setInfo(mc, "toString");
      NonPolymorphicMethodCall.setArgs(mc, args);
    }

    private boolean isStatic(IRNode context, IRNode bodyDecl) {
      if (bodyDecl == null) {
        throw new IllegalArgumentException("No enclosing body decl: " + context);
      }
      Operator op = JJNode.tree.getOperator(bodyDecl);
      if (MethodDeclaration.prototype.includes(op)) {
        return JavaNode.getModifier(bodyDecl, JavaNode.STATIC);
      } else if (ConstructorDeclaration.prototype.includes(op)) {
        return false;
      } else if (ClassInitializer.prototype.includes(op)) {
        return JavaNode.getModifier(bodyDecl, JavaNode.STATIC);
      } else if (FieldDeclaration.prototype.includes(op)) {
        return JavaNode.getModifier(bodyDecl, JavaNode.STATIC);
      } else if (VariableDeclarator.prototype.includes(op)) {
        return JavaNode.getModifier(VariableDeclarator.getMods(bodyDecl), JavaNode.STATIC);
      } else if (AnonClassExpression.prototype.includes(op)) {
        return false;
      } else if (EnumConstantDeclaration.prototype.includes(op)) {
        return true;
      }
      throw new IllegalArgumentException("Unexpected body decl: " + context);
    }

    /**
     * Create a ThisExpression or QualifiedThisExpression so that its type is
     * the type given (or a subtype)
     * 
     * @param context
     *          location in tree where expression will live
     * @param type
     *          required type of this expression
     * @param
     * @return new node for ThisExpression or QualifiedThisExpression or
     *         TypeExpression (if static)
     */
    protected IRNode createThisExpression(IRNode context, IJavaDeclaredType type, IRNode to) {
      boolean isStatic = to == null ? isStatic(context, VisitUtil.getEnclosingClassBodyDecl(context)) : isStatic(to, to);
      if (isStatic) {
        if (tEnv.findNamedType(type.getName()) == null) {
          final ITypeEnvironment tEnv2 = Projects.getEnclosingProject(to).getTypeEnv();
          if (tEnv2 != tEnv) {
            // HACK
            // This may introduce a new static dependency from this file to the
            // referred type
            // so we need to make sure that these types exist in the TEnv
            // (esp. if one canonicalized CU needs to be bound for another to be
            // used
            final IRNode cu = VisitUtil.findCompilationUnit(type.getDeclaration());
            tEnv.addTypesInCU(cu);
          }
        }
        final Operator op = JJNode.tree.getOperator(type.getDeclaration());
        if (/* AnonClassExpression.prototype.includes(op) || */EnumConstantClassDeclaration.prototype.includes(op)) {
          // Even though it's a static field, these local types can only have
          // instance methods
          return ThisExpression.prototype.createNode();
        }
        IRNode nt = CogenUtil.createNamedType(type.getDeclaration());
        addBinding(nt, IBinding.Util.makeBinding(type.getDeclaration()));
        return TypeExpression.createNode(nt);
      }
      IRNode thisClass = VisitUtil.getEnclosingType(context);
      IJavaType thisClassType = JavaTypeFactory.getMyThisType(thisClass, true, false);
      if (tEnv.isRawSubType(thisClassType, type)) {
        return ThisExpression.prototype.createNode();
      }
      IRNode qThis = thisClass;
      for (;;) {
        qThis = VisitUtil.getEnclosingType(qThis);
        if (qThis == null) {
          LOG.severe("Cannot find compatible This for " + type);
          return null;
        }
        IJavaType qThisType = JavaTypeFactory.getMyThisType(qThis);
        if (tEnv.isRawSubType(qThisType, type)) {
          IRNode nt = CogenUtil.createNamedType(qThis);
          return QualifiedThisExpression.createNode(nt);
        }
      }
    }

    protected boolean addImplicitOuterSpecifier(IRNode call) {
      IRNode p = tree.getParent(call);
      IRNode gp = tree.getParent(p);
      Operator pop = tree.getOperator(p);
      Operator gpop = tree.getOperator(gp);
      if (pop instanceof OuterObjectSpecifier || gpop instanceof OuterObjectSpecifier) {
        return false; // nothing to do.
      }
      // if this call is a newly constructed node, this may crash.
      // XXX: Right now I don't see any of these created in the code
      // XXX (especially since adding OOS is commented out for constructor
      // calls.)
      IBinding binding = binder.getIBinding(call);
      if (binding == null) {
        // TODO: handle this case, rather than give up.
        // XXX: Doing so requires knowing exactly how this node is created,
        // so we can avoid asking for types of new nodes.
        LOG.warning("Cannot handle adding OOS to " + DebugUnparser.toString(call));
        return false;
      }
      IJavaDeclaredType calleeThis = binding.getContextType();
      if (!(calleeThis instanceof IJavaNestedType))
        return false; // nothing to do
      if (JavaNode.getModifier(calleeThis.getDeclaration(), JavaNode.STATIC)) {
        return false;
      }
      Operator op = JJNode.tree.getOperator(calleeThis.getDeclaration());
      if (!(op instanceof NestedDeclInterface)) {
        return false;
      }
      IJavaNestedType calleeNested = ((IJavaNestedType) calleeThis);
      IRNode thisNode = createThisExpression(call, calleeNested.getOuterType(), null); // FIX
      if (pop instanceof AnonClassExpression) {
        call = p;
      }
      // first replace it and then set the nodes (so we don't lose our place in
      // the tree!)
      // XXX: have OOS been moved around when AnonClassExpression was
      // rearranged?
      IRNode newOOS = JavaNode.makeJavaNode(OuterObjectSpecifier.prototype);
      replaceSubtree(call, newOOS);
      OuterObjectSpecifier.setCall(newOOS, call);
      OuterObjectSpecifier.setObject(newOOS, thisNode);
      return true;
    }

    protected IRNode getImplicitSource(IRNode from, IRNode to) {
      IJavaDeclaredType type = (IJavaDeclaredType) JavaTypeFactory.getThisType(to);
      IRNode thisExpr = createThisExpression(from, type, to);
      if (thisExpr == null) {
        createThisExpression(from, type, to);
      } else {
        JavaNode.setImplicit(thisExpr);
      }
      // Need to copy to whole AST
      SkeletonJavaRefUtility.copyToTreeIfPossible(from, thisExpr);
      return thisExpr;
    }

    /**
     * Convert a name AST into an Expression AST
     * 
     * @param name
     *          a simple or qualified name AST node
     * @return an expression node
     */
    protected IRNode nameToExpr(IRNode name) {
      IRNode rv = null;
      try {
        IRNode decl = binder.getBinding(name);
        Operator op = tree.getOperator(decl);
        if (op instanceof TypeDeclInterface && !(op instanceof EnumConstantClassDeclaration)) {
          return rv = TypeExpression.createNode(nameToType(name, true));
        }
        String string = VariableDeclaration.getId(decl);
        if (op instanceof EnumConstantDeclaration) {
          IRNode implicitSource = getImplicitSource(name, decl);
          return rv = FieldRef.createNode(implicitSource, string);
        }
        if (tree.getOperator(name) instanceof SimpleName) {
          IRNode parent = tree.getParent(decl);
          IRNode gp = tree.getParent(parent);
          if (tree.getOperator(gp) instanceof FieldDeclaration) {
            /*
             * if ("oldException".equals(string)) {
             * System.out.println("Found ?.oldException"); }
             */
            IRNode implicitSource = getImplicitSource(name, decl);
            return rv = FieldRef.createNode(implicitSource, string);
          } else {
            return rv = VariableUseExpression.createNode(string);
          }
        } else {
          IRNode source = nameToExpr(QualifiedName.getBase(name));
          return rv = FieldRef.createNode(source, string);
        }
      } finally {
        if (rv != null) {
          SkeletonJavaRefUtility.copyIfPossible(name, rv);
          SkeletonJavaRefUtility.removeInfo(name);
        }
      }
    }

    /**
     * Convert a name AST into a Type AST.
     * 
     * @param nameNode
     *          a simple or qualified name AST node
     * @return a type AST node
     */
    protected IRNode nameToType(IRNode nameNode, boolean forceCreate) {
      IRNode result = null;
      if (QualifiedName.prototype.includes(nameNode)) {
    	  // TODO?
    	  IRNode base = nameToType(QualifiedName.getBase(nameNode), false);
    	  if (base != null) {
    		  String ref = QualifiedName.getId(nameNode);
    		  result = TypeRef.createNode(base, ref);
    	  }
    	  // Otherwise, this is the first one that should have a binding
      }
      if (result == null) {
    	  IBinding b = binder.getIBinding(nameNode);
    	  if (b == null) {
    		  if (forceCreate) {
    			  LOG.severe("Found no binding for " + DebugUnparser.toString(nameNode));
    			  result = createNamedType(DebugUnparser.toString(nameNode));
    		  }
    	  } 
    	  else if (TypeDeclaration.prototype.includes(b.getNode())) {
    		  result = createNamedType(nameNode, b);
    	  }
      }
      if (result == null) {
    	  return null;
      }
      if (!SkeletonJavaRefUtility.copyIfPossible(nameNode, result)) {
    	  LOG.warning("No java ref for "+DebugUnparser.toString(nameNode));
      }
      SkeletonJavaRefUtility.removeInfo(nameNode);      
      return result;
    }

    private IRNode createNamedType(String name) {     
      return NamedType.createNode(name);
    }

    /**
     * Derived from CogenUtil.createNamedType();
     */
    private IRNode createNamedType(IRNode nameNode, final IBinding b) {
      final IRNode tdecl = b.getNode();
      Operator op = JJNode.tree.getOperator(tdecl);
      IRNode result = null;
      try {
        if (op instanceof AnonClassExpression) {
          // Use base type
          IRNode base = AnonClassExpression.getType(tdecl);
          if (ParameterizedType.prototype.includes(base)) {
            base = ParameterizedType.getBase(base);
          }
          // FIX no bindings for subnodes
          return result = JJNode.copyTree(base);
        }
        String name = JJNode.getInfo(tdecl);
        if (op instanceof NestedTypeDeclInterface || op instanceof NestedEnumDeclaration
            || op instanceof NestedAnnotationDeclaration) {
          // Check if a local class
          IRNode enclosing = VisitUtil.getEnclosingClassBodyDecl(tdecl);
          if (enclosing != null
              && (SomeFunctionDeclaration.prototype.includes(enclosing) || ClassInitializer.prototype.includes(enclosing))
              || AnonClassExpression.prototype.includes(enclosing)) {
            // System.out.println("Converting type within a function");
            return result = createNamedType(name);
          }
          // Check if inside of an OOS expr
          /*
           * if ("Inner".equals(name)) {
           * System.out.println("Looking at name Inner"); }
           */
          IRNode parent = JJNode.tree.getParentOrNull(nameNode);
          if (NameType.prototype.includes(parent)) {
            IRNode gparent = JJNode.tree.getParentOrNull(parent);
            if (NewExpression.prototype.includes(gparent)) {
              IRNode ggparent = JJNode.tree.getParentOrNull(gparent);
              if (OuterObjectSpecifier.prototype.includes(ggparent)) {
                // No need to qualify this name then
                return result = createNamedType(name);
              }
            }
          }
          IJavaDeclaredType enclosingT = b.getContextType();
          IRNode baseType;
          if (enclosingT == null) {
            IRNode enclosingType = VisitUtil.getEnclosingType(tdecl);
            baseType = CogenUtil.createNamedType(enclosingType);
            addBinding(baseType, IBinding.Util.makeBinding(enclosingType));
          } else {
        	if (enclosingT.toString().equals("java.util.Map<K extends java.lang.Object in testGuava.MapConstraints.ConstrainedMultimap,java.util.Collection<V extends java.lang.Object in testGuava.MapConstraints.ConstrainedMultimap>>")) {
        		System.out.println("Found offending Map");
        	}
        	final boolean needsTypeParams = isNonstaticNestedClass(tdecl) && (nameNode == null || JavaTypeFactory.isRelatedTo(tEnv, nameNode, tdecl));
            baseType = createDeclaredType(enclosingT, needsTypeParams ? TypeParamHandling.KEEP : TypeParamHandling.DISCARD);
          }
          return result = TypeRef.createNode(baseType, name);
        }
        if (TypeUtil.isOuter(tdecl)) {
          String qname = TypeUtil.getQualifiedName(tdecl);
          qname = CommonStrings.intern(qname);
          return result = createNamedType(qname);
        }
        // LOG.warning("Creating NamedType: "+name);
        name = CommonStrings.intern(name);
        return result = createNamedType(name);
      } finally {
        if (result != null) {
          addBinding(result, b);
        }
      }
    }

    private boolean isNonstaticNestedClass(IRNode tdecl) {
    	// TODO TypeUtil.isStatic()?
    	return NestedClassDeclaration.prototype.includes(tdecl) && !JavaNode.getModifier(tdecl, JavaNode.STATIC);
	}

	// What if I have wildcards and other sorts of types?
    private IRNode createType(IJavaType t) {
      if (t == null) {
        return null;
      }
      if (t instanceof IJavaDeclaredType) {
        return createDeclaredType((IJavaDeclaredType) t, TypeParamHandling.KEEP_IF_NOT_ALL_FORMALS);
      }
      if (t instanceof IJavaTypeFormal) {
        IJavaTypeFormal f = (IJavaTypeFormal) t;
        String name = JJNode.getInfoOrNull(f.getDeclaration());
        IRNode result = createNamedType(name);
        addBinding(result, IBinding.Util.makeBinding(f.getDeclaration()));
        return result;
      }
      if (t instanceof IJavaWildcardType) {
        IJavaWildcardType w = (IJavaWildcardType) t;
        IRNode lower = createType(w.getUpperBound());
        if (lower != null) {
          return WildcardExtendsType.createNode(lower);
        }
        IRNode upper = createType(w.getLowerBound());
        if (upper != null) {
          return WildcardSuperType.createNode(upper);
        }
        return WildcardType.prototype.jjtCreate();
      }
      if (t instanceof IJavaArrayType) {
        IJavaArrayType a = (IJavaArrayType) t;
        IRNode base = createType(a.getBaseType());
        return ArrayType.createNode(base, a.getDimensions());
      }
      if (t instanceof IJavaPrimitiveType) {
        IJavaPrimitiveType p = (IJavaPrimitiveType) t;
        return p.getOp().createNode();
      }
      if (t instanceof IJavaCaptureType) {
        IJavaCaptureType c = (IJavaCaptureType) t;
        // TODO what to do about the capture bounds?
        return createType(c.getWildcard());
      }
      if (t instanceof IJavaVoidType) {
    	return VoidType.prototype.jjtCreate();
      }
      throw new IllegalStateException("Unexpected type: " + t);
    }
    
    private IRNode createDeclaredType(IJavaDeclaredType dt, TypeParamHandling typeParamHandling) {
      IRNode enclosingT = dt.getDeclaration();
      IBinding b;
      if (dt.getOuterType() != null) {
        b = IBinding.Util.makeBinding(enclosingT, dt.getOuterType(), tEnv);
      } else {
        b = IBinding.Util.makeBinding(enclosingT);
      }
      IRNode result = createNamedType(null, b);
      addBinding(result, b);
      if (dt.getTypeParameters().isEmpty()) {
        return result;
      }
      switch (typeParamHandling) {
      case DISCARD:
    	  return result;
      case KEEP_IF_NOT_ALL_FORMALS:
          // Don't keep parameters if all formals
          // BUT
          // Need to preserve parameters if it's surrounding a non-static class
          if (allTypeFormals(dt.getTypeParameters())) {
            return result;
          }          
      default:
    	  // keep going
      }
      
      IRNode[] args = new IRNode[dt.getTypeParameters().size()];
      for (int i = 0; i < args.length; i++) {
        args[i] = createType(dt.getTypeParameters().get(i));
      }
      IRNode rv = ParameterizedType.createNode(result, TypeActuals.createNode(args));
      JavaNode.setModifiers(rv, JavaNode.IMPLICIT);
      return rv;
    }

	private boolean allTypeFormals(List<IJavaType> typeParameters) {
      for (IJavaType tp : typeParameters) {
        if (tp instanceof IJavaTypeFormal) {
          continue;
        }
        return false;
      }
      return true;
    }

    @Override
    public Boolean visitExpression(IRNode node) {
      IRNode p = tree.getParent(node);
      boolean changed = false;
      if (!ExprStatement.prototype.includes(p)) {
        changed |= generateBoxUnbox(node);
      }
      changed |= super.visitExpression(node);
      return changed;
    }

    @Override
    public Boolean visitAddExpression(IRNode node) {
      LOG.finer("visiting add: " + node + ": " + DebugUnparser.toString(node));
      if (binder.getJavaType(node) == tEnv.getStringType()) {
        IRNode op1 = AddExpression.getOp1(node);
        IRNode op2 = AddExpression.getOp2(node);
        tree.removeSubtree(op1);
        tree.removeSubtree(op2);
        IRNode scnode = StringConcat.createNode(op1, op2);
        replaceSubtree(node, scnode);
        visitStringConcat(scnode);
        return true;
      } else {
        return super.visitAddExpression(node);
      }
    }

    @Override
    public Boolean visitOpAssignExpression(final IRNode node) {
      /*
       * If the type of the expression is not primitive, and not String, it must
       * be a boxed number or boxed boolean value.  In that case, we have to
       * change the operator type of the node to BoxedOpAssignExpression.
       * 
       * If the expression is "s += x", where s is a String, then we have
       * to update the OpAssignment to be a StringConcat, and make sure that
       * x is promoted to a String.
       */
      final JavaOperator op = OpAssignExpression.getOp(node);
      final IJavaType javaType = binder.getJavaType(node);
      if (javaType == tEnv.getStringType()) {
        if (op instanceof AddExpression) {
          // Reset the operator to StringConcat
          /* N.B. OpAssignExpression.getOp() synchronizes on the node, so we
           * do the same for setting the op. 
           */
          synchronized (node) {
            JavaNode.setOp(node, StringConcat.prototype);
          }
          
          // process the left-hand side
          doAccept(OpAssignExpression.getOp1(node));
          
          // Promote the right-hand side
          IRNode op2 = OpAssignExpression.getOp2(node);
          IJavaType t2 = binder.getJavaType(op2);
  
          // This *may* generate boxing
          LOG.finer("visiting second operand of opAssign SC: " + tree.getOperator(op2));
          doAccept(op2);
          
          // Need to be reloaded, since they might have been boxed, and may be
          // otherwise changed
          IRNode newOp2 = OpAssignExpression.getOp2(node);
          if (t2 instanceof IJavaPrimitiveType && !(tree.getOperator(newOp2) instanceof BoxExpression)) {
            boxExpression(newOp2);
            newOp2 = OpAssignExpression.getOp1(node);
          }
          generateToString(newOp2, t2);
          return true;
        }
      } else if (!(javaType instanceof IJavaPrimitiveType)) {
        final IRNode op1 = OpAssignExpression.getOp1(node);
        final IRNode op2 = OpAssignExpression.getOp2(node);
        tree.removeSubtree(op1);
        tree.removeSubtree(op2);
        final IRNode newNode = BoxingOpAssignExpression.createNode(op1, op, op2);
        replaceSubtree(node, newNode);
        super.visitOpAssignExpression(newNode);
        return true;
      }
      return super.visitOpAssignExpression(node);
    }

    @Override
    public Boolean visitAnonClassExpression(IRNode node) {
      // Reordered to process names before modifying 'extends' or 'implements'
      return doAcceptForChildren_rev(node);
    }

    @Override
    public Boolean visitArguments(IRNode node) {
      // Computing before canonicalizing the arguments
      final int numArgs = tree.numChildren(node);
      final IRNode lastArg = numArgs > 0 ? tree.getChild(node, numArgs - 1) : null;
      boolean changed = super.visitArguments(node);

      // Check for var args
      IRNode call = tree.getParent(node);
      IBinding b = binder.getIBinding(call);
      if (b == null) {
        return false;
      }
      final IRNode params = SomeFunctionDeclaration.getParams(b.getNode());
      final int numParams = tree.numChildren(params);
      if (numParams == 0) {
        // No varargs
        return changed;
      }
      final IRLocation last = tree.lastChildLocation(params);
      final IRNode lastP = tree.getChild(params, last);
      final IRNode lastType = ParameterDeclaration.getType(lastP);
      if (VarArgsType.prototype.includes(lastType)) {
        // System.out.println("Var binding: "+DebugUnparser.toString(b.getNode()));

        // Reorganize arguments
        final IRNode[] newArgs = new IRNode[numParams];
        final int numLastParam = numParams - 1;
        List<IRNode> varArgs;
        if (numArgs == numLastParam) {
          // Only non-varargs
          varArgs = Collections.emptyList();
        } else {
          if (numArgs == numParams) {
            // Check if using an array for the var args parameter
            IJavaType paramType = binder.getJavaType(lastP);
            IJavaType argType = binder.getJavaType(lastArg);
            if (tEnv.isCallCompatible(paramType, argType)) {
              // The last arg matches the var arg type, so no need to do
              // anything
              return changed;
            }
            IJavaArrayType varargsType = (IJavaArrayType) paramType;
            if (!tEnv.isCallCompatible(varargsType.getElementType(), argType)) {
              // Can't be an element of the varargs 
              return changed;
            }
          }
          varArgs = new ArrayList<IRNode>(numArgs - numLastParam);
        }

        int i = 0;
        for (IRNode arg : tree.children(node)) {
          if (i < numLastParam) {
            newArgs[i] = arg;
          } else {
            if (varArgs.isEmpty()) {
              // First var arg, so add cast here if needed
              // (using lastArg since we can't bind new nodes yet)
              final IJavaType argType = binder.getJavaType(lastArg);
              if (argType instanceof IJavaPrimitiveType) {
                final IJavaPrimitiveType primT = (IJavaPrimitiveType) argType;
                final IRNode lastBase = VarArgsType.getBase(lastType);
                final Operator op = tree.getOperator(lastBase);
                if (primT.getOp() != op && PrimitiveType.prototype.includes(op)) {
                  // Introduce cast to get the right type
                  tree.removeChild(node, arg);
                  arg = CastExpression.createNode(op.createNode(), arg);
                }
              }
            }
            varArgs.add(arg);
          }
          i++;
        }
        tree.removeChildren(node);
        IRNode varargsE = VarArgsExpression.createNode(varArgs.toArray(new IRNode[varArgs.size()]));
        if (!varArgs.isEmpty()) {
        	SkeletonJavaRefUtility.copyIfPossible(varArgs.get(0), varargsE);
        } else {
        	SkeletonJavaRefUtility.copyIfPossible(node, varargsE);
        }
        newArgs[numLastParam] = varargsE;
        replaceSubtree(node, Arguments.createNode(newArgs));
        changed = true;
      }
      return changed;
    }

    @Override
    public Boolean visitAssignExpression(IRNode node) {
      boolean changed = generateBoxUnbox(node);
      // we must visit the RHS first or else the box-unbox code for the RHS will
      // see
      // the changed LHS, and crash.
      changed |= doAccept(AssignExpression.getOp2(node));
      changed |= doAccept(AssignExpression.getOp1(node));
      return changed;
    }

    @Override
    public Boolean visitClassDeclaration(IRNode node) {
      // do default traversal (possibly finishing the constructor)
      boolean changed = super.visitClassDeclaration(node);
      // check to see if there is a constructor declaration, if not,
      // add a public parameterless one.
      // XXX: the parser really needs to do this: otherwise,
      // when the binder is called on the uncanonicalized code, it will fail to
      // find it.
      boolean found = false;
      IRNode body = ClassDeclaration.getBody(node);
      for (Iterator<IRNode> it = tree.children(body); it.hasNext();) {
        IRNode element = it.next();
        if (tree.getOperator(element) instanceof ConstructorDeclaration) {
          found = true;
          break;
        }
      }
      if (!found) {
        String name = JJNode.getInfo(node);
        final boolean isJavaLangObject;
        if ("Object".equals(name)) {
          final String qname = JavaNames.getQualifiedTypeName(node);
          isJavaLangObject = SLUtility.JAVA_LANG_OBJECT.equals(qname);
        } else {
          isJavaLangObject = false;
        }
        IRNode[] stmts;
        if (isJavaLangObject) {
          stmts = noNodes;
        } else {
          IRNode supercall = CogenUtil.makeDefaultSuperCall();
          stmts = new IRNode[] { supercall };
        }
        IRNode empty = MethodBody.createNode(BlockStatement.createNode(stmts));
        IRNode cdNode = CogenUtil.makeConstructorDecl(noNodes, JavaNode.PUBLIC, none, name, none, none, empty);
        ReceiverDeclaration.getReceiverNode(cdNode);
        ReturnValueDeclaration.getReturnNode(cdNode);
        tree.insertChild(body, cdNode);
        changed = true;
      }
      return changed;
    }

    @Override
    public Boolean visitConstructorCall(IRNode node) {
      return super.visitConstructorCall(node);
      // addImplicitOuterSpecifier(node);
    }

    @Override
    public Boolean visitConstructorDeclaration(IRNode node) {
      // check to see that first instruction is a ConstructorCall
      /*
       * It's a little complicated because: 1) we have to look in the
       * BlockStatement inside the MethodBody 2) we have to look inside the
       * first ExprStatement 3) the constructor call may be inside an
       * OuterObjectSpecifier On the other hand, if we generate a constructor
       * call, we can leave the inference of an OOS to visitConstructorCall.
       */
      IRNode mb = ConstructorDeclaration.getBody(node);
      if (tree.getOperator(mb) instanceof MethodBody) {
        IRNode bs = MethodBody.getBlock(mb);
        boolean found = false;
        if (tree.hasChildren(bs)) {
          IRNode first = tree.getChild(bs, 0);
          if (tree.getOperator(first) instanceof ExprStatement) {
            IRNode expr = ExprStatement.getExpr(first);
            if (tree.getOperator(expr) instanceof OuterObjectSpecifier) {
              expr = OuterObjectSpecifier.getCall(expr);
            }
            if (tree.getOperator(expr) instanceof ConstructorCall) {
              found = true;
            }
          }
        }
        // after determining *whether* we need to add something,
        // we do changes inside the constructor
        boolean changed = super.visitConstructorDeclaration(node);
        // and only then perform the change.
        if (!found) {
          IRNode type = VisitUtil.getEnclosingType(node);
          String name = JavaNames.getQualifiedTypeName(type);
          if (SLUtility.JAVA_LANG_OBJECT.equals(name)) {
            return changed;
          }
          // actually this will be rare because the parser stick in implicit
          // calls to super().
          LOG.fine("Canon: adding default super call");
          IRNode sc = CogenUtil.makeDefaultSuperCall();
          // IRNode es = ExprStatement.createNode(sc);
          tree.insertChild(bs, sc);
          changed = true;
        }
        return changed;
      } else {
        return super.visitConstructorDeclaration(node);
      }
    }

    @Override
    public Boolean visitConstructorReference(IRNode node) {
    	return handleMethodReference(node, ConstructorReference.getReceiver(node), "new", ConstructorReference.getTypeArgs(node));
    }
    
    @Override
    public Boolean visitForEachStatement(IRNode stmt) {
      IRNode expr = ForEachStatement.getCollection(stmt);
      IJavaType t = binder.getJavaType(expr);
      IRNode result;
      if (t instanceof IJavaArrayType) {
        result = createArrayLoopFromForEach(stmt, t);
      } else { // Assume to be Iterable
        result = createIterableLoopFromForEach(stmt, (IJavaReferenceType) t);
      }
      replaceSubtree(stmt, result);
      return true;
    }

    private IRNode makeDecl(int mods, String name, IRNode initE, IJavaType t) {
      IRNode type = createType(t);
      IRNode rv = makeDecl(mods, name, initE, type);
      SkeletonJavaRefUtility.copyIfPossible(initE, type);
      SkeletonJavaRefUtility.copyIfPossible(initE, rv);
      return rv;
    }

    private IRNode makeDecl(int mods, String name, IRNode expr, IRNode type) {
      IRNode init = Initialization.createNode(expr);
      IRNode vd = VariableDeclarator.createNode(name, 0, init);
      IRNode vars = VariableDeclarators.createNode(new IRNode[] { vd });
      IRNode rv = DeclStatement.createNode(Annotations.createNode(noNodes), mods, type, vars);
      return rv;
    }

    private IRNode adaptParamDeclToDeclStatement(IRNode pdecl, IRNode init) {
      int mods = ParameterDeclaration.getMods(pdecl);
      String name = ParameterDeclaration.getId(pdecl);
      IRNode type = ParameterDeclaration.getType(pdecl);
      tree.removeSubtree(type);
      // FIX destroy pdecl
      return makeDecl(mods, name, init, type);
    }

    private IRNode createArrayLoopFromForEach(IRNode stmt, final IJavaType collT) {
      // System.out.println("Translating array loop: "+stmt.toString());
      // handle children
      doAcceptForChildren(stmt);

      final String unparse = DebugUnparser.toString(stmt);

      // Create decl for array
      final String array = "array" + unparse.hashCode();
      IRNode collection = ForEachStatement.getCollection(stmt);
      tree.removeSubtree(collection);
      IRNode arrayDecl = makeDecl(JavaNode.FINAL, array, collection, collT);

      // Create decl for counter
      final String i = "i" + unparse.hashCode();
      IRNode iDecl = makeDecl(JavaNode.ALL_FALSE, i, IntLiteral.createNode("0"), IntType.prototype.jjtCreate());      
      
      // Create condition for while loop
      IRNode arrayLen = ArrayLength.createNode(VariableUseExpression.createNode(array));
      SkeletonJavaRefUtility.copyIfPossible(stmt, arrayLen);
      IRNode cond = LessThanExpression.createNode(VariableUseExpression.createNode(i), arrayLen);
      SkeletonJavaRefUtility.copyIfPossible(stmt, cond);

      // Create initializer for parameter
      IRNode paramInit = ArrayRefExpression
          .createNode(VariableUseExpression.createNode(array), VariableUseExpression.createNode(i));
      SkeletonJavaRefUtility.copyIfPossible(stmt, paramInit);
      
      IRNode whileLoop = makeEquivWhileLoop(stmt, cond, paramInit);
      IRNode result = BlockStatement.createNode(new IRNode[] { arrayDecl, iDecl, whileLoop });
      SkeletonJavaRefUtility.removeInfo(stmt);
      return result;
    }

    private IRNode makeEquivWhileLoop(IRNode stmt, IRNode cond, IRNode paramInit) {
      // Combine parameter and original body into the new body of the while loop
      IRNode paramDecl = adaptParamDeclToDeclStatement(ForEachStatement.getVar(stmt), paramInit);
      SkeletonJavaRefUtility.copyIfPossible(stmt, paramDecl);

      IRNode origBody = ForEachStatement.getLoop(stmt);
      tree.removeSubtree(origBody);
      IRNode body = BlockStatement.createNode(new IRNode[] { paramDecl, origBody });
      IRNode whileLoop = edu.cmu.cs.fluid.java.operator.WhileStatement.createNode(cond, body);
      SkeletonJavaRefUtility.copyIfPossible(stmt, whileLoop);
      return whileLoop;
    }

    private IRNode makeSimpleCall(final String object, String method) {
      return makeSimpleCall(VariableUseExpression.createNode(object), method);
    }

    private IRNode makeSimpleCall(IRNode object, String method) {
      IRNode args = Arguments.createNode(noNodes);
      IRNode rv = NonPolymorphicMethodCall.createNode(object, method, args);
      return rv;
    }

    /**
     * Used to help canonicalize for-each loops and the like
     * @author edwin
     */
    private final class MethodBinding implements IBinding {
      IJavaDeclaredType recType;
      IJavaTypeSubstitution subst;
      final ITypeEnvironment tEnv;
      final IRNode decl;
      final IJavaDeclaredType contextType;
      
      public MethodBinding(ITypeEnvironment te, IJavaDeclaredType t, IRNode n) {
      	if (t == null) {
    		throw new NullPointerException();
    	}    	  
      	tEnv = te;
        recType = t;
        decl = n;
        contextType = t;
      }

      @Override
      public IJavaReferenceType getReceiverType() {
        return recType;
      }

      public void updateRecType(IJavaDeclaredType t) {
        // FIX check t;
    	if (t == null) {
    		throw new NullPointerException();
    	}
        recType = t;
      }

      @Override
      public IJavaTypeSubstitution getSubst() {
    	IJavaDeclaredType ct = getContextType();   
    	if (subst == null) {
    	  if (ct != null) {
    		  subst = JavaTypeSubstitution.create(tEnv, ct);
    	  }
    	}
  		return subst;
      }
      
      @Override
      public IJavaType convertType(IBinder binder, IJavaType ty) {
        IJavaDeclaredType ct = getContextType();    	  
        if (ct != null && ct.isRawType(tEnv)) {
        	ty = tEnv.computeErasure(ty);
        }
        IJavaTypeSubstitution subst = getSubst();
        if (subst != null) {
          return Util.subst(ty, subst);
        }
        return ty;
      }
      
      @Override
      public IJavaDeclaredType getContextType() {
        return contextType;
      }

      @Override
      public IRNode getNode() {
        return decl;
      }
      
      @Override
      public String toString() {
    	return DebugUnparser.toString(decl);
      }
    }

    private MethodBinding findNoArgMethodInType(final IJavaDeclaredType type, final String name) {
      IRNode tdecl = type.getDeclaration();
      for (final IRNode m : VisitUtil.getClassMethods(tdecl)) {
        final String id = SomeFunctionDeclaration.getId(m);
        // System.out.println("Looking at "+id);
        if (name.equals(id)) {
          int numParams;
          if (ConstructorDeclaration.prototype.includes(m)) {
            numParams = JJNode.tree.numChildren(ConstructorDeclaration.getParams(m));
          } else {
            numParams = JJNode.tree.numChildren(MethodDeclaration.getParams(m));
          }
          if (numParams == 0) {
            return new MethodBinding(binder.getTypeEnvironment(), type, m);
          }
        }
      }
      return null;
    }

    private MethodBinding findNoArgMethod(final IJavaReferenceType type, final String name) {
      if (type == null) {
        return null;
      }
      final IJavaDeclaredType dType;
      if (type instanceof IJavaDeclaredType) {
        dType = (IJavaDeclaredType) type;
      } else {
        dType = null;
      }
      if (dType != null) {
        MethodBinding mb = findNoArgMethodInType(dType, name);
        if (mb != null) {
          return mb;
        }
      }
      for (IJavaType stype : type.getSupertypes(binder.getTypeEnvironment())) {
        MethodBinding mb = findNoArgMethod((IJavaDeclaredType) stype, name);
        if (mb != null) {
          if (dType != null) {
            mb.updateRecType(dType);
          }
          return mb;
        }
      }
      return null;
    }

    private IRNode createIterableLoopFromForEach(IRNode stmt, final IJavaReferenceType collT) {
      // System.out.println("Translating iterable loop: "+stmt.toString());
      final String unparse = DebugUnparser.toString(stmt);

      // Do any analysis before handling children
      IBinding mb = findNoArgMethod(collT, "iterator");
      if (mb == null) {
        findNoArgMethod(collT, "iterator");
        LOG.severe("Unable to find iterator() on " + collT);
        return null;
      }
      IRNode rtype = MethodDeclaration.getReturnType(mb.getNode());
      IJavaType rtypeT = binder.getJavaType(rtype);
      IJavaDeclaredType itTB = (IJavaDeclaredType) mb.convertType(binder, rtypeT);

      // handle children
      doAcceptForChildren(stmt);

      // Create decl for Iterable
      // final String iterable = "iterable"+unparse.hashCode();
      IRNode collection = ForEachStatement.getCollection(stmt);
      tree.removeSubtree(collection);

      // IRNode iterableDecl = makeDecl(JavaNode.FINAL, iterable, collection,
      // collT);
      // copySrcRef(stmt, iterableDecl);

      // Create decl for iterator
      final String it = "it" + unparse.hashCode();
      final IRNode itType = // CogenUtil.createType(binder.getTypeEnvironment(),
                            // itTB);
      // Iterator<?>
        ParameterizedType.createNode(NamedType.createNode("java.util.Iterator"),
            TypeActuals.createNode(new IRNode[] { WildcardType.prototype.jjtCreate() }));
      JavaNode.setModifiers(itType, JavaNode.IMPLICIT);

      IRNode itCall = makeSimpleCall(collection, "iterator");// makeSimpleCall(iterable,
                                                             // "iterator");
      SkeletonJavaRefUtility.copyIfPossible(stmt, itCall);

      IRNode itDecl = makeDecl(JavaNode.FINAL, it, itCall, itType);
      SkeletonJavaRefUtility.copyIfPossible(stmt, itDecl);

      // Create condition for while loop
      IRNode cond = makeSimpleCall(it, "hasNext");
      SkeletonJavaRefUtility.copyIfPossible(stmt, cond);

      // Create initializer for parameter
      IRNode paramInit = makeSimpleCall(it, "next");
      SkeletonJavaRefUtility.copyIfPossible(stmt, paramInit);

      /*
       * TODO is this necessary? // Introduce cast to the real type IRNode
       * castType = CogenUtil.createType(binder.getTypeEnvironment(),
       * computeIteratorType(binder.getTypeEnvironment(), itTB)); paramInit =
       * CastExpression.createNode(castType, paramInit);
       */

      IRNode whileLoop = makeEquivWhileLoop(stmt, cond, paramInit);
      IRNode result = BlockStatement.createNode(new IRNode[] { /* iterableDecl, */itDecl, whileLoop });
      SkeletonJavaRefUtility.removeInfo(stmt);
      return result;
    }

    private IJavaType computeIteratorType(ITypeEnvironment te, IJavaDeclaredType type) {
      if (type.getName().startsWith("java.util.Iterator")) {
        return type.getTypeParameters().get(0);
      }
      for (IJavaType st : type.getSupertypes(te)) {
        IJavaType value = computeIteratorType(te, (IJavaDeclaredType) st);
        if (value != null) {
          return value;
        }
      }
      return null;
    }

    @Override
    public Boolean visitImplicitReceiver(IRNode node) {
      IRNode parent = tree.getParent(node);
      IRNode method = binder.getBinding(parent);
      replaceSubtree(node, getImplicitSource(node, method));
      return true;
    }

    @Override
    public Boolean visitIncompleteThrows(IRNode node) {
      IRNode parent = tree.getParent(node);
      IRNode enclosingType = VisitUtil.getEnclosingType(parent);
      IRNode[] types = rewrite.makeDefaultThrows(enclosingType);
      replaceSubtree(node, Throws.createNode(types));
      return true;
    }
    
    @Override
	public Boolean visitLambdaExpression(IRNode node) {
    	//XXX: We assume already that "this" or "super" inside the L-E
    	// have been correctly bound / or will be correctly bound.
    	//XXX: What about marker interfaces in the interface type?
    	// Let's make sure that a marked ACE is not checked too strictly...
    	IJavaType ty = binder.getJavaType(node);
    	IJavaFunctionType fty = binder.getTypeEnvironment().isFunctionalType(ty);
    	// XXX: we should actually be making something that
    	// is an instance of of *all* these. 
    	IJavaDeclaredType base = null;
    	if (ty instanceof IJavaDeclaredType) {
    		base = (IJavaDeclaredType)ty;
    	} else if (ty instanceof IJavaIntersectionType) {
    		for (IJavaType ty1 : (IJavaIntersectionType)ty) {
    			if (binder.getTypeEnvironment().isFunctionalType(ty1) != null) {
    				if (ty1 instanceof IJavaDeclaredType) {
    					base = (IJavaDeclaredType)ty1;
    				}
    			}
    		}
    	}
    	if (base == null) {
    		LOG.severe("what is the interface?");
    		return false;
    	}
    	//XXX: Fix this to use a public interface.
    	String methodName = ((AbstractTypeEnvironment)binder.getTypeEnvironment()).getInterfaceSingleMethodSignatures(base.getDeclaration()).getName();
    	IRNode origBody = LambdaExpression.getBody(node);
    	doAccept(origBody);
    	origBody = LambdaExpression.getBody(node);
		JJNode.tree.removeSubtree(origBody);
		
    	IRNode newBody;
		if (Expression.prototype.includes(origBody)) {
			IRNode newStmt;
			if (fty.getReturnType() == JavaTypeFactory.voidType) {
				newStmt = ExprStatement.createNode(origBody);
			} else {
				newStmt = ReturnStatement.createNode(origBody);
			}
			newBody = MethodBody.createNode(BlockStatement.createNode(new IRNode[]{newStmt}));
		} else {
			newBody = origBody;
    	}
    	
    	List<IRNode> newParamList = new ArrayList<IRNode>();
    	Iterator<IJavaType> rqdit = fty.getParameterTypes().iterator();
    	for (IRNode formal : JJNode.tree.children(LambdaExpression.getParams(node))) {
    		IRNode ftype = ParameterDeclaration.getType(formal);
    		if (JJNode.tree.getOperator(ftype) == Type.prototype) {
    			IRNode ptype = createType(rqdit.next());
    			IRNode annos = Annotations.createNode(none); 
    			IRNode newParam = ParameterDeclaration.createNode(annos, JavaNode.ALL_FALSE, ptype, JJNode.getInfo(formal));
    			newParamList.add(newParam);
    		} else {
    			doAccept(formal);
    			JJNode.tree.removeSubtree(formal);
    			newParamList.add(formal);
    		}
    	}
    	IRNode newParams = Parameters.createNode(newParamList.toArray(none));
    	
    	List<IRNode> exceptionList = new ArrayList<IRNode>();
    	for (IJavaType tt : fty.getExceptions()) {
    		exceptionList.add(createType(tt));
    	}
    	IRNode exceptions = Throws.createNode(exceptionList.toArray(none));

    	// Possibly add @Override or Fluid annotations.
    	// What effects will be inferred ?
    	IRNode annos = Annotations.createNode(none);
    	IRNode types = TypeFormals.createNode(none);
    	int modifiers = JavaNode.ALL_FALSE|JavaNode.PUBLIC;
		IRNode rtype = createType(fty.getReturnType());
		
		IRNode mdecl = MethodDeclaration.createNode(annos,modifiers, types, rtype, methodName, newParams, 0, exceptions, newBody);
		IRNode cbody = ClassBody.createNode(new IRNode[]{mdecl});
		
		List<IRNode> typeArgList = new ArrayList<IRNode>();
		for (IJavaType t : base.getTypeParameters()) {
			typeArgList.add(createType(t));
		}
		final IRNode classType = createNamedType(null, IBinding.Util.makeBinding(base.getDeclaration()));
		final IRNode nexp;
		if (typeArgList.isEmpty()) {
			nexp = NonPolymorphicNewExpression.createNode(classType, Arguments.createNode(none));
		} else {
			final IRNode typeArgs = TypeActuals.createNode(typeArgList.toArray(none));		
			final IRNode paramdType = ParameterizedType.createNode(classType, typeArgs);
			nexp = NonPolymorphicNewExpression.createNode(paramdType, Arguments.createNode(none));
		}
		IRNode ace = AnonClassExpression.createNode(JavaNode.IMPLICIT, nexp, cbody);
		
		replaceSubtree(node,ace);
		if (!(fty.getReturnType() instanceof IJavaVoidType)) {
			ReturnValueDeclaration.makeReturnNode(mdecl);
		}
		ReceiverDeclaration.makeReceiverNode(mdecl);
		AbstractAdapter.createRequiredClassNodes(ace);
		PromiseUtil.addReceiverDeclsToType(ace);
		return true;
	}

    /*
    @Override
    public Boolean visitMethodCall(IRNode node) {
      final String unparse = DebugUnparser.toString(node);
      if (unparse.startsWith("annoType.getMethod(")) {
    	  System.out.println("Found annoType.getMethod(");
      }
      return super.visitMethodCall(node);
    }
    
	@Override
    public Boolean visitMethodDeclaration(IRNode node) {
	  final String name = JavaNames.genQualifiedMethodConstructorName(node);
	  if (name.contains("getValue(Attribute")) {
		  System.out.println("Canonicalizing "+name);
	  }
      return super.visitMethodDeclaration(node);
    }
	*/
	
    @Override
    public Boolean visitMethodReference(IRNode node) {
    	return handleMethodReference(node, MethodReference.getReceiver(node), MethodReference.getMethod(node), MethodReference.getTypeArgs(node));
    }

    boolean handleMethodReference(IRNode node, IRNode recv, String name, IRNode tArgs) {
    	boolean result = doAccept(recv);
    	result |= doAccept(tArgs);
    	// TODO
    	return result;
    }
    
    @Override
    public Boolean visitNameExpression(IRNode node) {
      /*
       * String unparse = DebugUnparser.toString(node); if
       * (unparse.contains("Project")) {
       * System.out.println("visitNameExpression: "+unparse); }
       */
      generateBoxUnbox(node); // generate boxing as needed
      IRNode name = NameExpression.getName(node);
      IBinding b = binder.getIBinding(name);
      if (b.getNode() == null) {
        return false;
      }
      IRNode replacement = nameToExpr(name);
      replaceSubtree(node, replacement);
      return true;
    }

    @Override
    public Boolean visitNameType(IRNode node) {
      /*
       * String unparse = DebugUnparser.toString(node); if
       * ("org.junit.runners.Parameterized.Parameters".equals(unparse)) {
       * System.
       * out.println("Visiting NameType: org.junit.runners.Parameterized.Parameters"
       * ); }
       */
      replaceSubtree(node, nameToType(NameType.getName(node), true));
      return true;
    }

    @Override
    public Boolean visitNewExpression(IRNode node) {
      IRNode old = NewExpression.getType(node);
      String unparse = DebugUnparser.toString(old);
      if ("SecureIterator".equals(unparse)) {
    	  System.out.println("Found Inner: "+DebugUnparser.toString(node)); 
      }      
      boolean changed = doAcceptForChildren_rev(node);
      if (changed) {
        map(old, NewExpression.getType(node));
      }
      changed |= addImplicitOuterSpecifier(node);
      return changed;
    }

    @Override
    public Boolean visitNonPolymorphicConstructorCall(IRNode node) {
      boolean changed = visitConstructorCall(node);
      // TODO perhaps replace with polyumorphic one
      // must do after the previous call because if there
      // are polymorphic parameters, we need to replace the node.
      return changed;
    }

    @Override
    public Boolean visitNonPolymorphicMethodCall(IRNode node) {
      boolean changed = generateBoxUnbox(node);
      // TODO perhaps replace with polymorphic one

      /*
       * FIX need to process children in reverse order? changed |=
       * doAccept(AssignExpression.getOp2(node)); changed |=
       * doAccept(AssignExpression.getOp1(node));
       */
      changed |= super.visitNonPolymorphicMethodCall(node);
      return changed;
    }

    @Override
    public Boolean visitNonPolymorphicNewExpression(IRNode node) {
      boolean changed = visitNewExpression(node);
      // TODO check to see if there are polymorphic implicit arguments.
      return changed;
    }
    
    @Override
    public Boolean visitStringConcat(IRNode node) {
      IRNode op1 = StringConcat.getOp1(node);
      IJavaType t1 = binder.getJavaType(op1);
      IRNode op2 = StringConcat.getOp2(node);
      IJavaType t2 = binder.getJavaType(op2);

      // This *may* generate boxing
      LOG.finer("visiting first operand of SC: " + tree.getOperator(op1));
      boolean changed = doAccept(op1);
      LOG.finer("visiting second operand of SC: " + tree.getOperator(op2));
      changed |= doAccept(op2);

      // Need to be reloaded, since they might have been boxed, and may be
      // otherwise changed
      IRNode newOp1 = StringConcat.getOp1(node);
      IRNode newOp2 = StringConcat.getOp2(node);

      if (t1 instanceof IJavaPrimitiveType && !(tree.getOperator(newOp1) instanceof BoxExpression)) {
        boxExpression(newOp1);
        newOp1 = StringConcat.getOp1(node);
        changed = true;
      }
      if (t2 instanceof IJavaPrimitiveType && !(tree.getOperator(newOp2) instanceof BoxExpression)) {
        boxExpression(newOp2);
        newOp2 = StringConcat.getOp2(node);
        changed = true;
      }

      changed |= generateToString(newOp1, t1);
      changed |= generateToString(newOp2, t2);
      return changed;
    }

    @Override
    public Boolean visitTypeDeclaration(IRNode node) {
      // Reordered to process names before modifying 'extends' or 'implements'
      return doAcceptForChildren_rev(node);
    }
  }

  // helper methods
  protected boolean isPrimitive(IRNode n) {
    return isPrimitive(n, binder.getJavaType(n));
  }

  protected boolean isPrimitive(IRNode n, IJavaType t) {
    try {
      return t instanceof IJavaPrimitiveType;
    } catch (RuntimeException e) {
      LOG.severe("Cannot tell primitive: " + DebugUnparser.toString(n));
      LOG.severe("Operator = " + JJNode.tree.getOperator(n));
      throw e;
    }
  }

  protected boolean isReference(IRNode n) {
    return binder.getJavaType(n) instanceof IJavaReferenceType;
  }

  protected boolean contextIsPrimitive(IRNode n) {
	synchronized (contextVisitor) {
		contextVisitor.loc = tree.getLocation(n);
		return contextVisitor.doAccept(tree.getParent(n)) == PRIMITIVE_CONTEXT;
	}
  }

  protected boolean contextIsReference(IRNode n) {
	synchronized (contextVisitor) {
		contextVisitor.loc = tree.getLocation(n);
		return contextVisitor.doAccept(tree.getParent(n)) == REFERENCE_CONTEXT;
	}
  }

  static final int REFERENCE_CONTEXT = 1;

  static final int PRIMITIVE_CONTEXT = -1;

  static final int ANY_CONTEXT = 0;

  @RegionLock("L is this protects Instance")
  class ContextVisitor extends Visitor<Integer> {
    IRLocation loc;

    // get the context value from this node's type
    private int contextFromNode(IRNode node) {
      return isPrimitive(node) ? PRIMITIVE_CONTEXT : REFERENCE_CONTEXT;
    }

    @Override
    public Integer visit(IRNode node) {
      LOG.warning("ContextVisitor didn't classify node with operator " + tree.getOperator(node));
      return ANY_CONTEXT;
    }

    @Override
    public Integer visitAddExpression(IRNode node) {
      if (binder.getJavaType(node) instanceof IJavaPrimitiveType) {
        return PRIMITIVE_CONTEXT;
      } else {
        return REFERENCE_CONTEXT;
      }
    }

    @Override
    public Integer visitAnonClassExpression(IRNode node) {
      return REFERENCE_CONTEXT;
    }

    @Override
    public Integer visitArguments(IRNode node) {
      IRNode call = tree.getParent(node);
      IRNode callee = binder.getBinding(call);
      if (callee == null)
        return ANY_CONTEXT;
      IRNode formals = SomeFunctionDeclaration.getParams(callee);
      final int i = tree.childLocationIndex(node, loc);
      final int n = tree.numChildren(formals);
      final int last = n - 1;
      if (i < last) {
        IRNode formal = tree.getChild(formals, i);
        return contextFromNode(formal);
      }
      // Could be varargs
      IRNode formal = tree.getChild(formals, last);
      IRNode fType = ParameterDeclaration.getType(formal);
      boolean vargs = VarArgsType.prototype.includes(fType);
      if (vargs) {
        /*
         * String unparse = DebugUnparser.toString(node); if
         * (unparse.contains("2, 3, 4")) {
         * System.out.println("Looking at VarArgsE: "+unparse); }
         */
        return contextFromNode(VarArgsType.getBase(fType));
      }
      // Not varargs, so do the same as above
      return contextFromNode(formal);
    }

    @Override
    public Integer visitArrayCreationExpression(IRNode node) {
      return ANY_CONTEXT;
    }

    @Override
    public Integer visitArrayInitializer(IRNode node) {
      IRNode p = tree.getParent(node);
      Operator pop = tree.getOperator(p);
      IJavaType atype;
      if (pop instanceof ArrayCreationExpression) {
        atype = binder.getJavaType(p);
      } else if (pop instanceof Initialization) {
        atype = binder.getJavaType(tree.getParent(p));
      } else if (pop instanceof ArrayInitializer) {
        atype = binder.getJavaType(p);
      } else if (pop instanceof ElementValuePair) {
    	//System.out.println("gparent = "+DebugUnparser.toString(tree.getParent(p)));
    	atype = binder.getJavaType(p);
      } else if (pop instanceof SingleElementAnnotation) {
    	//System.out.println("gparent = "+DebugUnparser.toString(tree.getParent(p)));
    	atype = binder.getJavaType(p);
      } else {
        throw new FluidError("ArrayInitializer inside a " + pop);
      }
      if (atype instanceof IJavaArrayType) {
        IJavaType etype = ((IJavaArrayType) atype).getElementType();
        return etype instanceof IJavaPrimitiveType ? PRIMITIVE_CONTEXT : REFERENCE_CONTEXT;
      } else {
        LOG.warning("Array initializer used in non-array context: " + DebugUnparser.toString(p));
        return ANY_CONTEXT;
      }
    }

    @Override
    public Integer visitArrayLength(IRNode node) {
      return REFERENCE_CONTEXT;
    }

    @Override
    public Integer visitArrayRefExpression(IRNode node) {
      if (loc == ArrayRefExpression.arrayLocation)
        return REFERENCE_CONTEXT;
      else if (loc == ArrayRefExpression.indexLocation)
        return PRIMITIVE_CONTEXT;
      else {
        LOG.severe("ArrayRef has a weird location: " + loc);
        return ANY_CONTEXT;
      }
    }

    @Override
    public Integer visitAssignExpression(IRNode node) {
      if (loc.equals(AssignExpression.op1Location))
        return ANY_CONTEXT;
      return contextFromNode(AssignExpression.getOp1(node));
    }

    @Override
    public Integer visitBinopExpression(IRNode node) {
      return PRIMITIVE_CONTEXT; // unless otherwise indicated
    }

    @Override
    public Integer visitBoxExpression(IRNode node) {
      return PRIMITIVE_CONTEXT;
    }

    @Override
    public Integer visitCastExpression(IRNode node) {
      return contextFromNode(node);
    }

    @Override
    public Integer visitClassExpression(IRNode node) {
      return REFERENCE_CONTEXT;
    }

    @Override
    public Integer visitConditionalExpression(IRNode node) {
      // Conditional section of the expression is always primitive
      if (loc.equals(ConditionalExpression.condLocation)) {
        return PRIMITIVE_CONTEXT;
      }
      
      if (binder.getJavaType(node) instanceof IJavaPrimitiveType) {
        return PRIMITIVE_CONTEXT;
      } else {
        return REFERENCE_CONTEXT;
      }
    }

    @Override
    public Integer visitConstantLabel(IRNode node) {
      return ANY_CONTEXT;
    }

    @Override
    public Integer visitConstructorCall(IRNode node) {
      return REFERENCE_CONTEXT;
    }

    @Override
    public Integer visitDefaultValue(IRNode node) {
      final IRNode annoElt = tree.getParent(node);
      final IRNode type = AnnotationElement.getType(annoElt);
      return doAccept(type);
      // return doAccept(DefaultValue.getValue(node));
    }

    @Override
    public Integer visitDimExprs(IRNode node) {
      return PRIMITIVE_CONTEXT;
    }

    @Override
    public Integer visitDoStatement(IRNode node) {
      return PRIMITIVE_CONTEXT;
    }

    @Override
    public Integer visitElementValuePair(IRNode node) {
      // TODO
      return PRIMITIVE_CONTEXT;
    }

    @Override
    public Integer visitStatementExpressionList(IRNode node) {
      return ANY_CONTEXT;
    }

    @Override
    public Integer visitEqualityExpression(IRNode node) {
      return ANY_CONTEXT;
    }

    @Override
    public Integer visitFieldRef(IRNode node) {
      return REFERENCE_CONTEXT;
    }

    @Override
    public Integer visitForEachStatement(IRNode node) {
      return REFERENCE_CONTEXT;
    }

    @Override
    public Integer visitForStatement(IRNode node) {
      if (loc.equals(ForStatement.condLocation)) {
        return PRIMITIVE_CONTEXT;
      }
      return ANY_CONTEXT;
    }

    @Override
    public Integer visitIfStatement(IRNode node) {
      return PRIMITIVE_CONTEXT;
    }

    @Override
    public Integer visitInitialization(IRNode node) {
      return contextFromNode(tree.getParent(node));
    }

    @Override
    public Integer visitInstanceOfExpression(IRNode node) {
      return REFERENCE_CONTEXT;
    }

    @Override
    public Integer visitLambdaExpression(IRNode node) {
      return REFERENCE_CONTEXT;   
    }
    
    @Override
    public Integer visitMethodCall(IRNode node) {
      return REFERENCE_CONTEXT;
    }

    @Override
    public Integer visitOpAssignExpression(IRNode node) {
      // TODO: Can't really handle this:
      // What about
      // { Integer x; ... x += 34; }
      // We put an UnboxExpression around the LHS which is strange.
      if (binder.getJavaType(node) == tEnv.getStringType()) {
        return REFERENCE_CONTEXT;
      } else {
        return PRIMITIVE_CONTEXT;
      }
    }

    @Override
    public Integer visitOuterObjectSpecifier(IRNode node) {
      return REFERENCE_CONTEXT;
    }

    @Override
    public Integer visitParenExpression(IRNode node) {
      return ANY_CONTEXT;
    }

    @Override
    public Integer visitPrimLiteral(IRNode node) {
      return PRIMITIVE_CONTEXT;
    }

    @Override
    public Integer visitPrimitiveType(IRNode node) {
      return PRIMITIVE_CONTEXT;
    }

    @Override
    public Integer visitReferenceType(IRNode node) {
      return REFERENCE_CONTEXT;
    }

    @Override
    public Integer visitReturnStatement(IRNode node) {
      IJavaType rtype = binder.getJavaType(node);
      return rtype instanceof IJavaPrimitiveType ? PRIMITIVE_CONTEXT : REFERENCE_CONTEXT;
    }

    @Override
    public Integer visitSingleElementAnnotation(IRNode node) {
      IBinding b = binder.getIBinding(node);
      IRNode body = AnnotationDeclaration.getBody(b.getNode());
      for (IRNode m : tree.children(body)) {
        String name = AnnotationElement.getId(m);
        if (AnnotationConstants.VALUE_ATTR.equals(name)) {
          return doAccept(AnnotationElement.getType(m));
        }
      }
      return ANY_CONTEXT;
    }

    @Override
    public Integer visitStatement(IRNode node) {
      return ANY_CONTEXT;
    }

    @Override
    public Integer visitStringConcat(IRNode node) {
      return REFERENCE_CONTEXT;
    }

    @Override
    public Integer visitUnboxExpression(IRNode node) {
      return REFERENCE_CONTEXT;
    }

    @Override
    public Integer visitUnopExpression(IRNode node) {
      return PRIMITIVE_CONTEXT;
    }

    @Override
    public Integer visitVarArgsExpression(IRNode node) {
      return REFERENCE_CONTEXT;
    }

    @Override
    public Integer visitWhileStatement(IRNode node) {
      return PRIMITIVE_CONTEXT;
    }
  }

  private final ContextVisitor contextVisitor = new ContextVisitor();

}
