package edu.cmu.cs.fluid.java.util;

import java.util.*;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.FluidError;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.bind.IBindHelper;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.PromiseConstants;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.promise.*;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.*;

/**
 */
public final class PromiseUtil {
  /** Logger instance for debugging. */
  private static final Logger LOG = SLLogger.getLogger("FLUID.util.PromiseUtil");

  // Collections used to return all the nodes added to the tree, whether as
  // code or as promises
  private static final Collection<IRNode> dummy = new Vector<IRNode>();

  // FIX required promises not getting added to anon classes
  public static void addRequiredTypePromises(IBinder binder, 
    IBindHelper lh,
    IRNode ast,
    Collection<IRNode> added) {
    // force init methods to be precomputed
    // (versioning problems)
    IRNode init = JavaPromise.getClassInitOrNull(ast);
    if (init == null) {
    	init = ClassInitDeclaration.getClassInitMethod(ast);    
    	added.add(init);
    }

    // TODO No receiver on ClassInit?
    // added.add(ReceiverDeclaration.getReceiverNode(init));

    Operator op = PromiseConstants.tree.getOperator(ast);
    if (!InterfaceDeclaration.prototype.includes(op)) {
      IRNode n = JavaPromise.getInitMethodOrNull(ast);
      if (n == null) {
    	  n = InitDeclaration.getInitMethod(ast);
    	  added.add(n); // not needed for interfaces
      }      
      addQualifiedReceiverDeclToType(ast, added);
    }

    // No longer desirable to link * regions between classes
    /*
		 * IRNode superB = lh.getSuperclass(ast); IRNode superRegion = null; if
		 * (superB != null) { // not java.lang.Object String sName = (String)
		 * superB.getSlotValue(TypeEnvironment.qnameSI); IRNode superT =
		 * CogenUtil.createTypeE(sName); "); } ", superRegion);
		 * JavaPromise.addClassRegion(ast, rd);
		 */
  }

  /**
	 * force receiver and return to be precomputed (versioning problems)
	 */
  public static void addRequiredMethodPromises(IBinder binder, 
    IRNode node,
    boolean isConstructor,
    Collection<IRNode> added) {

    if (isConstructor) {
    	addReceiverDeclsToConstructor(node, added);
    } 
    else if (!JavaNode.getModifier(node, JavaNode.STATIC)) {
    	addReceiverDeclsToMethod(node, added);
    }
//    if (!isConstructor) {
//      IRNode type  = MethodDeclaration.getReturnType(node);      
//      if (!(JJNode.tree.getOperator(type) instanceof VoidType)) {    
    	  // constructors and void methods don't have a return node
    IRNode rvd = JavaPromise.getReturnNodeOrNull(node);
    if (rvd == null) {
        added.add(ReturnValueDeclaration.getReturnNode(node));
    }
//      }
//    }
  }

  public static void addRequiredInitializerPromises(IBinder binder, 
    IRNode init,
    Collection<IRNode> added) {
    /*
    if (!JavaNode.getModifier(init, JavaNode.STATIC)) {
      addReceiverDecls(init, added);
    }
    */
  }
  
  private static Collection<IRNode> nullCollection = new AbstractCollection<IRNode>() {
      @Override
      public boolean add(IRNode e) { return false; }              
      
      @Override
      public Iterator<IRNode> iterator() {
        return new EmptyIterator<IRNode>();
      }

      @Override
      public int size() {
        return 0;
      }
  };
  
  public static void addReceiverDeclsToMethod(IRNode method) { 
	  addReceiverDeclsToMethod(method, nullCollection);
  }
  
  // Really ok for any decl that should have one
  public static void addReceiverDeclsToMethod(IRNode method, Collection<IRNode> added) {
	  final IRNode recv = JavaPromise.getReceiverNodeOrNull(method);
	  if (recv == null) {
		  final Operator op = JJNode.tree.getOperator(method);
		  if (InitDeclaration.prototype.includes(op)) {
			  added.add(ReceiverDeclarationInInit.makeReceiverNodeForInit(method));
		  } else {
			  added.add(ReceiverDeclaration.getReceiverNode(method));
		  }
	  }
  }
  
  public static void addReceiverDeclsToConstructor(IRNode constructor) { 
	addReceiverDeclsToConstructor(constructor, nullCollection);
  }

  // Only statically visible ones?
  private static void addReceiverDeclsToConstructor(IRNode here, Collection<IRNode> added) {    
	addReceiverDeclsToMethod(here, added);
	  
	final IRNode enclosingT = VisitUtil.getEnclosingType(here);
	if (enclosingT == null) {
        LOG.severe("No enclosing types for "+DebugUnparser.toString(here));
        VisitUtil.getEnclosingTypes(here, false);
        return;
    }
	addQualifiedReceiverDecl(enclosingT, here, added);
  }
  
  public static void addReceiverDeclsToType(IRNode type) {
	  addReceiverDeclsToMethod(type, nullCollection);
	  addQualifiedReceiverDeclToType(type, nullCollection);
  }
  
  private static void addQualifiedReceiverDeclToType(IRNode type, Collection<IRNode> added) {
	  addQualifiedReceiverDecl(type, type, added);
  }
  
  private static void addQualifiedReceiverDecl(IRNode innerT, IRNode toBePromisedFor, Collection<IRNode> added) {
	  if (TypeUtil.isStatic(innerT)) {
		  return; // No QR
	  }
	  final IRNode outerT = VisitUtil.getEnclosingType(innerT);    
	  if (outerT != null) {
		  //final String outerTName = JavaNames.getFullTypeName(outerT);
		  IRNode qrecv = JavaPromise.getQualifiedReceiverNodeOrNull(toBePromisedFor);
		  if (qrecv == null) {
			  added.add(QualifiedReceiverDeclaration.makeReceiverNode(toBePromisedFor, outerT));
		  }
	  }
  }
  
  /**
	 * A form of activateTypePromises, but simplified for class files
	 * 
	 * This sets up the required promises for the type, its constructors, and
	 * methods
	 */
  public static void activateRequiredPromises(IBinder binder, 
    IBindHelper helper,
    IRNode type,
    Collection<IRNode> added) {
    addRequiredTypePromises(binder, helper, type, added);

    IRNode cbody = VisitUtil.getClassBody(type);
    Iterator<IRNode> enm = PromiseConstants.tree.children(cbody);
    while (enm.hasNext()) {
      IRNode node = enm.next();
      Operator op = PromiseConstants.tree.getOperator(node);

      if (op == ConstructorDeclaration.prototype) {
        //System.out.println("Looking at a constructor");
        PromiseUtil.addRequiredMethodPromises(binder, node, true, added);
      } else if (op == MethodDeclaration.prototype) {
        PromiseUtil.addRequiredMethodPromises(binder, node, false, added);
      } else if (op == ClassInitializer.prototype) {
        PromiseUtil.addRequiredInitializerPromises(binder, node, added);
      } else if (op == FieldDeclaration.prototype) {
        // no need to do anything
      } else if (
        op == NestedClassDeclaration.prototype
          || op == NestedInterfaceDeclaration.prototype
          || op == AnonClassExpression.prototype
          || op == NestedAnnotationDeclaration.prototype) {
        addRequiredTypePromises(binder, helper, node, added);
      } else if (EnumDeclaration.prototype.includes(op)) {
        addRequiredTypePromises(binder, helper, node, added);
      } else if (EnumConstantDeclaration.prototype.includes(op)) {
        addRequiredEnumConstantDeclPromises(binder, helper, node, added, op);
      } else if (AnnotationElement.prototype.includes(op)) {
        System.err.println("Ignoring required promises on "+op.name());
      }
      
      /*
			 * FIX make sure that anon/inner classes get properly init'd w/ promises
			 * http://boiling.fluid.cs.cmu.edu/bugs/show_bug.cgi?id=88
			 */
      else {
        throw new FluidError("Unexpected ClassBodyDecl :" + op);
      }
    }
  }
  
  private static void addRequiredEnumConstantDeclPromises(IBinder binder, IBindHelper helper, IRNode node, 
                                                          Collection<IRNode> added,
                                                          Operator op) {
    if (!EnumConstantClassDeclaration.prototype.includes(op)) {
//      if (JJNode.tree.numChildren(node) > 0) {
//    	  IRNode body = EnumConstantDeclaration.getBody(node);    
//    	  if (!NoClassBody.prototype.includes(body)) {
//    		  LOG.severe("Got an EnumConstantDecl with a class body that's not a TypeDecl");
//    	  }
//      }
      return;
    }
    addRequiredTypePromises(binder, helper, node, added);
  }

  /**
	 * Sets up required promises for a compilation unit
	 * 
	 * @param cu
	 *          A compilation unit
	 */
  public static void activateRequiredCuPromises(IBinder binder, 
    IBindHelper helper,
    IRNode cu,
    Collection<IRNode> added) {
    // iterate over the top-level types in the compilation unit
    // Iterator enum = VisitUtil.getTypeDecls(cu);
    Iterator<IRNode> enm = JJNode.tree.topDown(cu);
    while (enm.hasNext()) {
      IRNode type = enm.next();
      Operator op = JJNode.tree.getOperator(type);
      /*
      if (op instanceof TypeDeclarationStatement) {
        LOG.info("Found a TypeDeclStmt for "+DebugUnparser.toString(type));
      }
      */
      if (!(op instanceof TypeDeclInterface)) {
        continue; // Not a type
      }
      if (op instanceof TypeFormal) {
        continue; // No required promises needed
      }
      // LOG.info("Activating promises on "+DebugUnparser.toString(type));
      activateRequiredPromises(binder, helper, type, added);
    }
  }

  /** @deprecated */
  @Deprecated
  public static void addRequiredTypePromises(IBinder binder, IBindHelper lh, IRNode ast) {
    addRequiredTypePromises(binder, lh, ast, dummy);
    dummy.clear();
  }

  /** @deprecated */
  @Deprecated
  public static void addRequiredMethodPromises(IBinder binder,
    IRNode node,
    boolean isConstructor) {
    addRequiredMethodPromises(binder, node, isConstructor, dummy);
    dummy.clear();
  }

  /** @deprecated */
  @Deprecated
  public static void activateRequiredPromises(IBinder binder, 
    IBindHelper helper,
    IRNode type) {
    activateRequiredPromises(binder, helper, type, dummy);
    dummy.clear();
  }

  /** @deprecated */
  @Deprecated
  public static void activateRequiredCuPromises(IBinder binder, 
    IBindHelper helper,
    IRNode cu) {
    activateRequiredCuPromises(binder, helper, cu, dummy);
    if (!dummy.isEmpty()) {
    	Collection<IRNode> nodes = dummy;
    	System.out.println("Got "+nodes.size()+" added nodes");
    }
    dummy.clear();
  }

  /**
   * Get the {@link MethodDeclaration} or {@link ConstructorDeclaration} that
   * contains the given expression. The given expression is expected to be, or
   * to be inside of either a {@link ConstructorDeclaration},
   * {@link MethodDeclaration}, {@link FieldDeclaration}, or
   * {@link ClassInitializer}. Each class has a unique ReceiverDeclaration
   * associated with it used to represent the receiver for instance field
   * initializers and for instance class initializer blocks. In particular, this
   * method differs from {@link VisitUtil#getEnclosingMethod(IRNode)} because it
   * returns the special "class initialization" method declaration node for
   * nodes that are part of an instance field declaration or instance
   * initialization block.
   * 
   * @param expr
   *          The expression to get the enclosing method/constructor declaration
   *          for. This node is expected either to be, or to be contained in a
   *          {@link ConstructorDeclaration}, {@link MethodDeclaration},
   *          {@link FieldDeclaration}, or {@link ClassInitializer}.
   * 
   * @return The enclosing method/constructor declaration or <code>null</code>
   *         if the given node is inside a static field declaration or class
   *         initialization block.
   */
  public static IRNode getEnclosingMethod(final IRNode expr) {
    IRNode current = expr;
    while (current != null) {
      final Operator op = JJNode.tree.getOperator(current);
      if (MethodDeclaration.prototype.includes(op)
          || ConstructorDeclaration.prototype.includes(op)) {
        return current;
      } else if (FieldDeclaration.prototype.includes(op)
          || ClassInitializer.prototype.includes(op)) {
        if (TypeUtil.isStatic(current)) {
          // Static contexts don't have a method
          return null;
        } else {
          // Get the class's initialization receiver
          final IRNode type = VisitUtil.getEnclosingType(current);
          return JavaPromise.getInitMethodOrNull(type);
        }
      }
      current = JJNode.tree.getParentOrNull(current);
    }
    throw new IllegalArgumentException("Couldn't find enclosing method for " + DebugUnparser.toString(expr));
  }
 
  /**
   * Get the {@link ReceiverDeclaration} that is appropriate for the context
   * in which given expression is located. The expression ought to be inside of
   * either a {@link ConstructorDeclaration}, {@link MethodDeclaration},
   * {@link FieldDeclaration}, or {@link ClassInitializer}. Each method and
   * constructor declaration has a unique ReceiverDeclaration associated
   * with it. Each class has a unique hidden method declaration associated with it
   * used to represent the context for instance field intializers and for
   * instance class initializer blocks.
   * 
   * @return The receiver node for the context containing the giving expression,
   *         or <code>null</code> if the expression is inside a static
   *         context.
   */
  public static IRNode getReceiverNode(final IRNode expr) {
    final IRNode mdecl = getEnclosingMethod(expr);
    if (mdecl == null) {
      return null;
    } else {
      return TypeUtil.isStatic(mdecl) ? null : JavaPromise.getReceiverNodeOrNull(mdecl);
    }
  }

  /**
   * Get the {@link QualifiedReceiverDeclaration} for the given qualifying type
   * that is appropriate for the context in which given expression is located.
   * The expression ought to be inside of either a
   * {@link ConstructorDeclaration}, {@link MethodDeclaration},
   * {@link FieldDeclaration}, or {@link ClassInitializer}. Each method and
   * constructor declaration has a unique QualifiedReceiverDeclaration for each
   * of it's outer classes associated with it. Each class has a unique hidden
   * method declaration associated with it used to represent the context for
   * instance field intializers and for instance class initializer blocks.
   * 
   * @return The qualified receiver node of given qualifying type for the
   *         context containing the giving expression, or <code>null</code> if
   *         the expression is inside a static context.
   */
  public static IRNode getQualifiedReceiverNode(
      final IRNode expr, final IRNode qualifyingType) {
    final IRNode mdecl = getEnclosingMethod(expr);
    if (mdecl == null) {
      return null;
    } else {
      return TypeUtil.isStatic(mdecl) ? null : JavaPromise.getQualifiedReceiverNodeByName(mdecl, qualifyingType);
    }
  }
}