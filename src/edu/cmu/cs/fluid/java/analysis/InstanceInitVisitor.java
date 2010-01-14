/*
 * Created on Mar 2, 2005
 */
package edu.cmu.cs.fluid.java.analysis;

import java.util.Iterator;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.operator.BlockStatement;
import edu.cmu.cs.fluid.java.operator.ConstructorCall;
import edu.cmu.cs.fluid.java.operator.ConstructorDeclaration;
import edu.cmu.cs.fluid.java.operator.OmittedMethodBody;
import edu.cmu.cs.fluid.java.operator.Visitor;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;

/**
 * Visitor for traversing non-static Instance initializers. Call
 * doVisitInstanceInits from your traversal's visitConstructorDecl method.
 *
 * <p><em>This class does not work as advertised.  It didn't make sense to fix
 * it.  Instead a new class {@link InstanceInitializationVisitor} should be 
 * used.</em>
 * 
 * <p>Your visitor must do three things:
 * 
 * <p>B1: You must keep track of the most closely nested InstanceInitVisitor
 * (relevant code marked "B1" in the snippets below.
 * 
 * <p>B2: You must over-ride visitConstructorDeclaration. Whatever else your
 * visitor does here, it must also create a new InstanceInitVisitor, save the
 * old InstanceInitVisitor, call doVisitInstanceInits(), and restore the old
 * InstanceInitVisitor. Code marked "B2" in the snippets below.
 * 
 * <p>B3: You must over-ride visitConstructorCall. In addition to your own
 * analysis, you must call initHelper.doVisitInstanceInits() right after the
 * traversal of the constructorCall. Code marked B3 in the snippets below.
 * 
 * <pre>
 * 
 *  class CfpTreeWalker extends Visitor {
 *  
 *     ...
 *     InstanceInitVisitor initHelper = null; // B1
 *     ...
 *  
 *     public void visitConstructorDeclaration(IRNode root) { // B1
 *       final InstanceInitVisitor saveInitHelper = initHelper; // B1
 *  
 *       try {
 *          initHelper = new InstanceInitVisitor(cfpTW); // B2
 *          // note that doVisitInstanceInits will only do the traversal when
 *          // appropriate, and will call back into this visitor to travers the
 *          // inits themselves.
 *          initHelper.doVisitInstanceInits(root); // B2
 *  
 *        } finally {
 *          initHelper = saveInitHelper;  // B1
 *        }
 *      }
 *  
 *  
 *     public void visitConstructorCall(IRNode node) {  // B3
 *        ...
 *        super.visitConstructorCall(node);
 *  
 *        if (initHelper != null) {					  // B3
 *          initHelper.doVisitInstanceInits(node);	  // B3
 *       } 										  // B3
 *       ...
 *      }
 *  }
 *  
 * </pre>
 * 
 * <p>The call to doVisitInstanceInits in visitConstructorDeclaration will figure
 * out where in the tree the traversal of the instance init code should be done.
 * There are only three choices -- first thing in the constructor, just after
 * the call to super() which is the first thing in the constructor, or not at
 * all. In the first case, the doVisitInstanceInits call from the
 * constructorDecl will kick off the traversal right away. In the second case,
 * the call from visitConstructorCall will take care of it (while carefully
 * doing nothing just after any _other_ constructorCall you encounter). In the
 * third case, it will appropriately do nothing at either call site.
 * 
 * @author dfsuther
 * 
 */
@Deprecated
public class InstanceInitVisitor<T> extends Visitor<T> {

  // by default, do nothing

  final Visitor<T> analysisWeAreHelping;

  IRNode firstSuperCall = null;

  /** The {@link ClassBody} node that contains the constructor.  This node
   * is <em>not</em> the ClassDeclaration that contains the constructor,
   * which is good because this makes it easier to avoid stepping into
   * nested classes.
   */
  IRNode enclosingClass = null;

  public InstanceInitVisitor(Visitor<T>  yourAnalysis) {
    analysisWeAreHelping = yourAnalysis;
  }

  public void clear() {
	  firstSuperCall = null;
	  enclosingClass = null;
  }
  
  /**
   * Possibly kick off traversal of Instance field initializations. Kicks off a
   * traversal that will visit the instance field initializers when appropriate
   * according to Java semantics. This method should be invoked inside the
   * parent visitor's visitConstructorDecl method, before the body of the
   * constructor is traversed.
   * 
   * @param constructorDeclOrCall
   *          The constructor declaration the outer analysis visitor is working
   *          on now.
   */
  public void doVisitInstanceInits(IRNode constructorDeclOrCall) {
    final Operator op = JJNode.tree.getOperator(constructorDeclOrCall);
    if (ConstructorDeclaration.prototype.includes(op)) {

      // now that we know we really are a constructor, find the class and
      // traverse looking only for inits...
      IRNode p = JJNode.tree.getParent(constructorDeclOrCall); // assume not
                                                                  // null
      enclosingClass = p;

      // check to see whether we need to traverse instance inits at all...

      final IRNode body = ConstructorDeclaration.getBody(constructorDeclOrCall);
      final Operator bodyOp = JJNode.tree.getOperator(body);
      if (body == null || OmittedMethodBody.prototype.includes(bodyOp)) {
        // need to traverse inits, have no constructor call. We'll do the
        // traversal
        // now!!!
        firstSuperCall = null;
        this.doAcceptForChildren(p);
        return;
      } else if (BlockStatement.prototype.includes(bodyOp)) {
        // found a block. If the first thing in the block is a call to a
        // constructor in the same class, that other constructor will handle the
        // inits, so they need not be traversed here.
        Iterator<IRNode> bl = BlockStatement.getStmtIterator(body);
        if (bl.hasNext()) {
          final IRNode firstInBody = bl.next();

          final Operator firstOp = JJNode.tree.getOperator(firstInBody);
          if (ConstructorCall.prototype.includes(firstOp)) {
            IRNode cDecl = ConstructorCall.getObject(firstInBody);
            IRNode p1 = JJNode.tree.getParent(cDecl);
            if (p1.equals(p)) {
              // we're calling a constructor in the same class, so no need to
              // do the init traversal for this constructor at all.
              firstSuperCall = null;
              return;
            } else if (p1.equals(JJNode.tree.getParent(p)) ) {
              // we're calling a constructor that is from super(), so we need to
              // do the init traversal right after the firstInBody.
              firstSuperCall = firstInBody;
              return;
            }
            // fall through to do the init traversal now...
          }
        }
      }

      // traverse all instance initialization
      firstSuperCall = null;
      this.doAcceptForChildren(p);
    } else if (ConstructorCall.prototype.includes(op)) {
      // we've been invoked after a ConstructorCall. Check to see if this is the
      // specific call that must be followed by traversing the init code; if so,
      // traverse the init code.
      if (constructorDeclOrCall.equals(firstSuperCall)) {
        this.doAcceptForChildren(enclosingClass);
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see edu.cmu.cs.fluid.java.operator.Visitor#visit(edu.cmu.cs.fluid.ir.IRNode)
   */
  @Override
  public T visit(IRNode node) {
    // visit all children by default
    super.doAcceptForChildren(node);
    return null;
  }

  // for initializers and field declarations, check if static
  @Override
  public T visitClassInitializer(IRNode node) {
    if (!JavaNode.getModifier(node, JavaNode.STATIC)) {
      // no null-check, because we want to fail fast.
      analysisWeAreHelping.doAcceptForChildren(node);
    }
    return null;
  }

  @Override
  public T visitFieldDeclaration(IRNode node) {
    if (!JavaNode.getModifier(node, JavaNode.STATIC)) {
      // no null-check, because we wish to fail fast if wrong!
      analysisWeAreHelping.doAcceptForChildren(node);
    }
    return null;
  }

  @Override
  public T visitAnonClassExpression(final IRNode expr) {
    // Don't go inside type/class declarations, it will only confuse us
    return null;
  }

  @Override
  public T visitClassDeclaration(final IRNode expr) {
    // Don't go inside type/class declarations, it will only confuse us
    return null;
  }

  @Override
  public T visitEnumDeclaration(final IRNode expr) {
    // Don't go inside type/class declarations, it will only confuse us
    return null;
  }

  @Override
  public T visitInterfaceDeclaration(final IRNode expr) {
    // Don't go inside type/class declarations, it will only confuse us
    return null;
  }
}
