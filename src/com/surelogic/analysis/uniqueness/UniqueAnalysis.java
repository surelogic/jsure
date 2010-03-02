/*
 * $Header:
 * /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/analysis/UniqueAnalysis.java,v
 * 1.53 2003/10/31 15:18:30 chance Exp $
 */
package com.surelogic.analysis.uniqueness;

import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.analysis.IBinderClient;
import com.surelogic.analysis.bca.BindingContextAnalysis;
import com.surelogic.analysis.effects.*;
import com.surelogic.analysis.effects.targets.Target;
import com.surelogic.analysis.regions.*;
import com.surelogic.annotation.rules.UniquenessRules;
import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.FluidError;
import edu.cmu.cs.fluid.control.*;
import edu.cmu.cs.fluid.control.Component.WhichPort;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.PlainIRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.analysis.FakeBinder;
import edu.cmu.cs.fluid.java.analysis.IntraproceduralAnalysis;
import edu.cmu.cs.fluid.java.analysis.JavaEvaluationTransfer;
import edu.cmu.cs.fluid.java.analysis.TestEvaluationTransfer;
import edu.cmu.cs.fluid.java.bind.AbstractBinder;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IBinding;
import edu.cmu.cs.fluid.java.bind.IJavaArrayType;
import edu.cmu.cs.fluid.java.bind.IJavaDeclaredType;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.bind.ISuperTypeSearchStrategy;
import edu.cmu.cs.fluid.java.bind.ITypeEnvironment;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.promise.ReceiverDeclaration;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.*;
import edu.cmu.cs.fluid.version.Era;
import edu.cmu.cs.fluid.version.Version;
import static edu.cmu.cs.fluid.java.JavaGlobals.noNodes;

/**
 * Analysis for determining whether an expression evaluates to a unique value.
 * 
 * <p>
 * An expression is unique if all the objects it can be evaluated to were
 * unique when they occured (factory method, unique constructor, unique
 * parameters or unique fields) and have not been <em>compromised</em>
 * since. A reference is compromised if it is stored in an object (except in
 * certain cases for unique fields) or if it is passed to a method without
 * being limited. A reference is unique if all its aliases are known to be
 * local variables.
 * </p>
 * 
 * <p>
 * We use an abstract store to keep track of which variables reference which
 * objects. A forward flow analysis translates Java actions to store actions.
 * The store keeps track of a stack of evaluated references; thus we must use
 * an analysis that correctly handles stack depth.
 * <p>
 * 
 * @see Store
 * @see JavaEvaluationTransfer
 * @see #isUnique
 */
public class UniqueAnalysis extends IntraproceduralAnalysis<Object,Boolean> 
implements IBinderClient {
  /** Logger instance for debugging. */
  private static final Logger LOG = SLLogger.getLogger("FLUID.analysis.unique");

  /**
	 * Return value for {@link #getNormalErrorMessage}and
	 * {@link #getAbruptErrorMessage}for the case when there is no error.
	 */
  public static final String NOT_AN_ERROR = "Usage is correct.";

  private final Effects effects;
  
  
  
  public UniqueAnalysis(IBinder binder, Effects e) {
    super(new FixBinder(binder)); // avoid crashes.
    effects = e;
  }

  public IBinder getBinder() {
	  return this.binder;
  }
  
  public void clearCaches() {
	  effects.clearCaches();
	  clear();
  }
  
  /**
	 * Return whether the evaluation of this expression always is a unique
	 * reference.
	 */
  public boolean isUnique(IRNode node, final IRNode constructorContext) {
    Store s = (Store) getAnalysisResultsAfter(node, constructorContext);
    if (s == null)
      return false;
    if (!s.isValid())
      return false;
    int n = ((Integer) s.getStackSize().getValue()).intValue();
    return n > 0 && s.isUnique();
  }

  /**
	 * Return whether the evaluation of this expression always is storeable (not
	 * limited)
	 */
  public boolean isStoreable(IRNode node, final IRNode constructorContext) {
    Store s = (Store) getAnalysisResultsAfter(node, constructorContext);
    if (s == null)
      return false;
    if (!s.isValid())
      return false;
    int n = ((Integer) s.getStackSize().getValue()).intValue();
    return n > 0 && s.isStoreable();
  }

  /*****************************************************************************
	 * Return whether the expression is *not
	 */
  public boolean isLimited(IRNode node, final IRNode constructorContext) {
    Store s = (Store) getAnalysisResultsAfter(node, constructorContext);
    if (s == null)
      return false;
    if (!s.isValid())
      return false;
    int n = ((Integer) s.getStackSize().getValue()).intValue();
    return n > 0 && !s.isStoreable();
  }

  /**
	 * Return whether the evaluation of this expression leads to a problem (store
	 * being made invalid).
	 */
  public boolean isInvalid(final IRNode node, final IRNode flowUnit) {
    final FlowAnalysis a = getAnalysis(flowUnit);
    final Store sbefore = (Store) a.getAfter(node, WhichPort.ENTRY);
    final Store safter = (Store) a.getAfter(node, WhichPort.NORMAL_EXIT);
    final Store sabrupt = (Store) a.getAfter(node, WhichPort.ABRUPT_EXIT);

    // A node is invalid if things were OK when the store
    // came in, but are wrong now that control is leaving.
    //? NB: top() sometimes means control didn't get to
    //? a place but also happens near the start of a procedure
    //? before OpStart() and so we can't ignore top(). Also,
    //? I believe top().isValid is false. This is all the
    //? more confusing because top() means what is usually
    //? meant by bottom for analysis people. JTB 2002/9/23

    // If the state coming in is bad, no error:
    if (sbefore != null
      && !sbefore.equals(sbefore.top())
      && !sbefore.isValid())
      return false;

    // If the state coming out for normal termination is bad, an error:
    if (safter != null && !safter.equals(safter.top()) && !safter.isValid())
      return true;

    // If the state coming out for abrupt termination is bad, an error:
    if (sabrupt != null
      && !sabrupt.equals(sabrupt.top())
      && !sabrupt.isValid())
      return true;

    // Otherwise, must be OK
    return false;
  }

  /**
	 * Return whether the expression is positively assured by analysis. This
	 * method only makes sense when invoked on a
	 * <ul>
	 * <li>A field store, i.e., an assignment expression whose lhs is a FieldRef
	 * node.
	 * <li>A method call
	 * <li>A method body
	 * </ul>
	 * 
	 * @param node
	 *          An parse node representing the expression to test.
	 * @return (Fill this in)
	 */
  public boolean isPositivelyAssured(final IRNode node, final IRNode flowUnit) {
    final FlowAnalysis a = getAnalysis(flowUnit);
    final Store safter = (Store) a.getAfter(node, WhichPort.NORMAL_EXIT);
    final Store sabrupt = (Store) a.getAfter(node, WhichPort.ABRUPT_EXIT);
    final boolean afterOK =
      (safter == null) || safter.equals(sabrupt.top()) || safter.isValid();
//      (safter != null) && safter.isValid();
    final boolean abruptOK =
      (sabrupt == null) || sabrupt.equals(sabrupt.top()) || sabrupt.isValid();

    return afterOK && abruptOK;
  }

  /**
	 * Get the error message for an invalid normal terminination. If the normal
	 * termination is not erroneous gives an appropriate non-alarming message.
	 * Does not check to see if the error originated from a surrounding node.
	 * Usage should be similar to
	 * 
	 * <pre>
	 *  UniqueAnalysis ua = ...; ... if( ua.isInvalid( n ) ) { // get the reason String errString = ua.getNormalErrorMessage( n ); String errString2 = ua.getAbruptErrorMessage( n ); ... }
	 *     * </pre>
	 * 
	 * @see #isInvalid
	 * @see #getAbruptErrorMessage
	 * @see #NOT_AN_ERROR
	 */
  /* Based on implementation of isInvalid. Make sure to keep in sync! */
  public String getNormalErrorMessage(final FlowAnalysis a, final IRNode node) {
    final Store safter = (Store) a.getAfter(node, WhichPort.NORMAL_EXIT);
    // If the state coming out for normal termination is bad, an error:
    if (safter != null && !safter.equals(safter.top()) && !safter.isValid()) {
      return safter.toString();
    } else {
      return NOT_AN_ERROR;
    }
  }

  /**
	 * Get the error message for an invalid abrupt terminination. If the abrupt
	 * termination is not erroneous gives an appropriate non-alarming message.
	 * Does not check to see if the error originated from a surrounding node.
	 * Usage should be similar to
	 * 
	 * <pre>
	 *  UniqueAnalysis ua = ...; ... if( ua.isInvalid( n ) ) { // get the reason String errString = ua.getNormalErrorMessage( n ); String errString2 = ua.getAbruptErrorMessage( n ); ... }
	 *     * </pre>
	 * 
	 * @see #isInvalid
	 * @see #getNormalErrorMessage
	 * @see #NOT_AN_ERROR
	 */
  /* Based on implementation of isInvalid. Make sure to keep in sync! */
  public String getAbruptErrorMessage(final FlowAnalysis a, final IRNode node) {
    final Store sabrupt = (Store) a.getAfter(node, WhichPort.ABRUPT_EXIT);
    // If the state coming out for normal termination is bad, an error:
    if (sabrupt != null
      && !sabrupt.equals(sabrupt.top())
      && !sabrupt.isValid()) {
      return sabrupt.toString();
    } else {
      return NOT_AN_ERROR;
    }
  }

  /**
	 * Create a flow analysis to check unique references. We gather together all
	 * the locals (including parameters) and create a store. Fortunately most of
	 * the work of checking annotations is handled by the store.
	 * 
	 * @see Store
	 * @see Store#opStart
	 */
  @Override
  protected FlowAnalysis createAnalysis(IRNode flowNode) {
    if (LOG.isLoggable(Level.FINE)) {
      LOG.fine("Starting uniqueness analysis on " + DebugUnparser.toString(flowNode));
    }
    FlowUnit op = (FlowUnit) tree.getOperator(flowNode);
    final Store store = new Store(flowUnitLocals(flowNode, false, binder));
    FlowAnalysis analysis =
      new ForwardAnalysis(
        "unique analysis",
        store,
        new UniqueTransfer(this, flowNode, binder, effects), 
        DebugUnparser.viewer) {
      @Override
      protected void usePorts(
        boolean secondary,
        OutputPort port,
        InputPort dual,
        LabelList ll,
        Lattice value) {
        if (LOG.isLoggable(Level.FINE)
          && port instanceof AbruptExitPort
          && ll.equals(LabelList.empty)) {
          LOG.fine(
            "Empty LabelList for: "
              + DebugUnparser.toString(port.getSyntax())
              + " with Lattice:\n"
              + value);
        }
        super.usePorts(secondary, port, dual, ll, value);
      }
    };
    analysis.initialize(op.getSource(flowNode).getOutput(), store.opStart());
    return analysis;
  }
}

class UniqueTransfer extends JavaEvaluationTransfer {
  /** Logger instance for debugging. */
  private static final Logger LOG =
	  SLLogger.getLogger("FLUID.analysis.unique.transfer");

  private final Effects effects;
  private final IRNode flowUnit;
  
  public UniqueTransfer(UniqueAnalysis ua, IRNode fu, IBinder b, Effects e) {
    super(ua, b);
    effects = e;
    flowUnit = fu;
  }

  /** Pop and discard value from top of stack */
  @Override
  protected Lattice pop(Lattice val) {
    Store s = (Store) val;
    return s.pop();
  }

  /** Push an unknown shared value onto stack. */
  @Override
  protected Lattice push(Lattice val) {
    Store s = (Store) val;
    return s.opExisting(Store.sharedVariable);
  }

  /** Remove the second from top value from stack */
  @Override
  protected Lattice popSecond(Lattice val) {
    Store s = (Store) val;
    if (!s.isValid())
      return s;
    /* stack[n-1] = top of stack */
    return s.opSet(s.getUnderTop());
  }

  @Override
  protected Lattice dup(Lattice val) {
    Store s = (Store) val;
    if (!s.isValid())
      return s;
    return s.opGet(s.getStackTop());
  }

  /** Remove all pending values from stack */
  @Override
  protected Lattice popAllPending(Lattice val) {
    Store s = (Store) val;
    while (s.isValid() && s.getStackTop().intValue() > 0) {
      s = s.pop();
    }
    return s;
  }

  /**
	 * Return a store after taking into acount these effects (usually inferred
	 * from a method call).
	 */
  protected Store considerEffects(
    IRNode rcvr,
    IRNode actuals,
    Set<Effect> effects,
    Store s) {
    int n = tree.numChildren(actuals);
    for (Iterator<Effect> fx = effects.iterator(); fx.hasNext();) {
      try {
        Effect f = fx.next();

        if (f.isReadEffect()) {
          // case 1: using permissions:
          // we only can bury aliases if we have write permission,
          // so we can ignore this case.
          // but with alias burying, we cannot ignore reads
          //CAN'T: continue;
        }

        Target t = f.getTarget();
        IRNode ref = t.getReference();

	      if (ref != null) {
          // case 2:
          // if we are referencing a parameter or receiver, it may be
          // it is a final class with no unique fields.
          IJavaType ty = binder.getJavaType(ref);
          if (ty instanceof IJavaArrayType) {
            // case 2a: if a "unique Array", then problems.
            // otherwise done
            //! No unique array types (I think) ?
            continue;
          }
          if (ty instanceof IJavaDeclaredType) {
            IRNode cd = ((IJavaDeclaredType) ty).getDeclaration();

            if (cd != null
                && JJNode.tree.getOperator(cd) instanceof ClassDeclaration
                && JavaNode.getModifier(cd, JavaNode.FINAL)) {
              boolean hasUnique = regionHasUniqueFieldInClass(t.getRegion(), cd);
              if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("Effect " + f + " is on final class with"
                    + (hasUnique ? "" : "out") + " unique fields");
              }
              if (!hasUnique) continue;
            }
          }
        }

        // From this point assume we are going to negate everything
        // reachable from the target.

        // First we load the reference of the target
        if (ref == null) {
          s = s.opExisting(Store.sharedVariable);
        } else if (ref.equals(rcvr)) {
          s = s.opDup(n);          
        } else {
          foundActual: {
            for (int i = 0; i < n; ++i) {
              if (tree.getChild(actuals, i).equals(ref)) {
                s = s.opDup(n - i + 1);
                break foundActual;
              }
            }
            s = s.opExisting(Store.sharedVariable);
          }
        }
        
        IRegion r = t.getRegion();
        if (r.isAbstract()) {
          // it could refer to almost anything
          // everything reachable from this pointer is alias buried:
          s = s.opLoadReachable();
        } else {
          // it's a field, load and discard.
          s = s.opLoad(r.getNode()).pop();
        }
      } catch (ClassCastException ex) {
        // shouldn't happen, but safe to ignore exception
        LOG.log(Level.WARNING, "Expected effect but got exception ", ex);
      }
    }
    return s;
  }

  /**
   * Return true if the given region may have a field in the given class
   * that is unique.
   */
  protected boolean regionHasUniqueFieldInClass(IRegion reg, IRNode cd) {
    // TODO define this method
    //
    // One possibility is to enumerate the fields of the class
    // and ask each one if they are in the region.
    //
    //! For now, assume the worst:
    return true;
  }

  /** Return the store after popping off and processing each actual
   * parameter according to the formal parameters.  The number should
   * be the same (or else how did the binder determine to call
   * this method/constructor?).
   */
  protected Store popArguments(IRNode actuals, IRNode formals, Store s) {
    int n = tree.numChildren(actuals);
    if (formals != null && n != tree.numChildren(formals)) {
      throw new FluidError("#formals != #actuals");
    }
    while (n-- > 0) {
      IRNode formal = formals != null ? tree.getChild(formals, n) : null;
      if (formal != null && UniquenessRules.isUnique(formal)) {
        s = s.opUndefine();
      } else if (formal == null || UniquenessRules.isBorrowed(formal)) {
        s = s.opBorrow();
      } else {
        s = s.opCompromise();
      }
    }
    return s;
  }

  /**
	 * The top of the stack was a receiver of a method call, pop it off using one
	 * of the three methods for popping depending on the promises/demands about
	 * the receiver.
	 */
  protected Store popReceiver(IRNode decl, Store s) {
    if (decl == null)
      return s.opBorrow();
    if (JavaNode.getModifier(decl, JavaNode.STATIC)) {
      return s.opRelease();
    } else {
      boolean isConstructor = ConstructorDeclaration.prototype.includes(decl);
      IRNode recDecl = JavaPromise.getReceiverNode(decl);
      IRNode retDecl = JavaPromise.getReturnNode(decl);
      
      if (UniquenessRules.isUnique(recDecl)) {
        return s.opUndefine();
      } else if (UniquenessRules.isBorrowed(recDecl) ||
          (isConstructor && UniquenessRules.isUnique(retDecl))) {
        return s.opBorrow();
      } else {
        if (isConstructor) {
          if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Receiver is not limited for\n  " + DebugUnparser.toString(decl));
          }
        }
        return s.opCompromise();
      }
    }
  }

  /**
	 * Push an abstract value corresponding to the return decl of the method or
	 * constructor declared in decl.
	 */
  protected Store pushReturnValue(IRNode decl, Store s) {
    if (decl == null)
      return s.opNew(); // expect the best
    IRNode ty = MethodDeclaration.getReturnType(decl); // XXX: May crash if it's a ConstructorDeclaration
    if (tree.getOperator(ty) instanceof VoidType)
        return s.push();
    IRNode retDecl = JavaPromise.getReturnNodeOrNull(decl);
    if (retDecl == null) {
        LOG.severe("Method missing return value declaration");
        return s.push();
    }
    // IRNode ty = ReturnValueDeclaration.getType(retDecl);
    if (UniquenessRules.isUnique(retDecl)) {
      return s.opNew();
    } else if (tree.getOperator(ty) instanceof ReferenceType) {
      return s.opExisting(Store.sharedVariable);
    } else {
      return s.push(); // push non-object
    }
  }

  @Override protected Lattice transferAllocation(IRNode node, Lattice value) {
    Store s = (Store) value;
    return s.opNew();
  }

  @Override protected Lattice transferAnonClass(IRNode node, Lattice value) {
    Store s = (Store) value;
    // first gather up all variables used in the body and
    // compromise them.
    //! NB: If we binding does this for us, we can use
    //! a faster enumeration.
    for (Iterator<IRNode> e = tree.bottomUp(AnonClassExpression.getBody(node));
      e.hasNext();
      ) {
      IRNode n = e.next();
      if (VariableUseExpression.prototype.includes(tree.getOperator(n))) {
        IRNode decl = binder.getBinding(n);
        if (decl == null) {
          LOG.warning("No binding for " + DebugUnparser.toString(node));
        } else {
          // if undefined, then tough, it's an error.
          s = s.opGet(decl).opCompromise();
        }
      }
    }
    // Now compromise "this" (this is slightly more conservative than necessary)
    s = s.opThis().opCompromise();
    return s;
  }

  @Override protected Lattice transferArrayCreation(IRNode node, Lattice val) {
    // inefficient but simple:
    Store s = (Store) super.transferArrayCreation(node, val);
    return s.pop().opNew();
  }

  @Override protected Lattice transferArrayInitializer(IRNode node, Lattice val) {
    Store s = (Store) val;
    return s.opCompromise();
  }

  @Override protected Lattice transferAssignArray(IRNode node, Lattice val) {
    Store s = (Store) super.transferAssignArray(node, val);
    return s.opCompromiseNoRelease();
  }

  @Override protected Lattice transferAssignField(IRNode node, Lattice val) {
    IRNode fieldDecl = binder.getBinding(node);
    if (fieldDecl == null) {
      LOG.warning("field not bound " + DebugUnparser.toString(node));
      return popSecond(val);
    } else {
      Store s = (Store) val;
      if (!s.isValid())
        return s;
      Integer object = s.getUnderTop();
      Integer field = s.getStackTop();
      // first copy both onto stack:
      s = s.opGet(object).opGet(field);
      // now perform assignment:
      s = s.opStore(fieldDecl);
      // now pop extraneous object off stack:
      return popSecond(s);
    }
  }

  @Override protected Lattice transferAssignVar(IRNode var, Lattice val) {
    IRNode varDecl = binder.getBinding(var);
    if (varDecl == null) {
      LOG.warning(
        "No binding for assigned variable " + DebugUnparser.toString(var));
    } else {
      Store s = (Store) val;
      if (!s.isValid())
        return s;
      s = (Store) dup(s);
      return s.opSet(varDecl);
    }
    return val;
  }

  @Override protected Lattice transferCall(IRNode node, boolean flag, Lattice value) {
    IRNode mdecl = binder.getBinding(node);
    Operator op = tree.getOperator(node);
    boolean mcall = MethodCall.prototype.includes(op);
//    boolean ncall = NewExpression.prototype.includes(op);
    Store s = (Store) value;
    
    CallInterface call = (CallInterface) op;
    IRNode actuals     = call.get_Args(node);
    IRNode formals     = null;
    if (mdecl == null) {
      LOG.warning("No binding for method " + DebugUnparser.toString(node));
    } else {
      if (tree.getOperator(mdecl) instanceof ConstructorDeclaration) {
        // LOG.fine("Before ccall: store is " + s);
        formals = ConstructorDeclaration.getParams(mdecl);
      } else {
        formals = MethodDeclaration.getParams(mdecl);
      }
    }
    IRNode receiverNode = mcall ? ((MethodCall) call).get_Object(node) : null;
    if (mdecl != null) {
      /* If the flowunit is a class body, then we are dealing with instance
       * initialization and the caller is the <init> method.
       */
      final IRNode caller; 
      if (ClassBody.prototype.includes(flowUnit)) {
        caller = JavaPromise.getInitMethod(JJNode.tree.getParent(flowUnit));
      } else {
        caller = flowUnit;
      }
      s =
        considerEffects(
          receiverNode,
          actuals,
          effects.getMethodCallEffects(node, caller, true),
          s);
    }
    // we have to possibly compromise arguments:
    s = popArguments(actuals, formals, s);
    if (hasOuterObject(node)) {
      if (LOG.isLoggable(Level.FINE))
        LOG.fine("Popping qualifiers!");
      if (!s.isValid())
        return s;
      // compromise value under top
      // 1. copy it onto top,
      // 2. compromise new top and discard it
      // 3. popSecond
      s = s.opGet(s.getUnderTop());
      s = s.opCompromise();
      if (!s.isValid()) return s;
      /* This does a popSecond by popping the top value off the stack (op set)
       * and changing the variable just under the top to have the value
       * that was on the top of the stack.
       */
      s = s.opSet(s.getUnderTop());
    }
    if (!mcall) {
      // we keep a copy of the object being constructed
      s = (Store) dup(s);
    }
    // for method calls (need return value)
    // for new expressions (object already duplicated)
    // and for constructor calls (also already duplicated)
    // we pop the receiver.
    s = popReceiver(mdecl, s);
    
    if (LOG.isLoggable(Level.FINE)) {
      LOG.fine("After handling receivers/qualifiers/parameter: "+ s);
    }
    if (flag) { // call succeeded
      if (mcall) {
        s = pushReturnValue(mdecl, s);
      }
      return s;
    } else {
      // we just pop all pending
      s = (Store) popAllPending(s); 
      return s;
    }
  }

  @Override protected Lattice transferCloseScope(IRNode node, Lattice val) {
    /* Nullify all variables that were in scope.
     * NB: "undefined" would be closer in semantics, but this is not done
     * to check errors, but rather for efficiency and null is
     * much cheap in the semantics than undefined.  Java's scope rules
     * ensure that we don't.
     */
    Store s = (Store) val;
    for (Iterator<IRNode> e = BlockStatement.getStmtIterator(node); e.hasNext();) {
      IRNode stmt = e.next();
      if (JJNode.tree.getOperator(stmt) instanceof DeclStatement) {
        IRNode vars = DeclStatement.getVars(stmt);
        for (Iterator<IRNode> e2 = VariableDeclarators.getVarIterator(vars); e2.hasNext();) {
          IRNode var = e2.next();
          s = s.opNull();
          s = s.opSet(var);
        }
      }
    }
    return s;
  }
  
  @Override protected Lattice transferDefaultInit(IRNode node, Lattice val) {
    Store s = (Store) val;
    IRNode ty = VariableDeclarator.getType(tree.getParent(node));
    if (tree.getOperator(ty) instanceof ReferenceType) {
      return s.opNull();
    } else {
      return s.push(); // not an object
    }
  }

  @Override protected Lattice transferEq(IRNode node, boolean flag, Lattice value) {
    // compare top two stack values and pop off.
    // then push a boolean value (unused).
    return push(((Store) value).opEqual(flag));
  }

  @Override protected Lattice transferFailedCall(IRNode node, Lattice value) {
    throw new FluidError("execution should not reach here.");
  }

  @Override protected Lattice transferInitializationOfField(IRNode node, Lattice value) {
    final boolean fineIsLoggable = LOG.isLoggable(Level.FINE);
    Store s = (Store) value;
    // get class or "this" on stack
    if (Store.isStaticField(node)) {
      s = s.opExisting(Store.sharedVariable);
      if (fineIsLoggable) {
        LOG.fine("initializing static field '" + JJNode.getInfo(node) + "'");
      }
    } else {
      s = s.opThis();
      if (fineIsLoggable) {
        LOG.fine("initializing field '" + JJNode.getInfo(node) + "'");      
      }
    }
    s = s.opDup(1); // get value on top of stack
    s = s.opStore(node); // perform store
    s = s.opRelease(); // throw away extra copy
    return s;
  }

  @Override protected Lattice transferInitializationOfVar(IRNode node, Lattice value) {
    Store s = (Store) value;
    return s.opSet(node);
  }

  @Override protected Lattice transferLiteral(IRNode node, Lattice value) {
    Operator op = tree.getOperator(node);
    Store s = (Store) value;
    if (NullLiteral.prototype.includes(op)) {
      return s.opNull();
    } else if (RefLiteral.prototype.includes(op)) {
      return push(s); // push a shared String constant
    } else {
      return s.push(); // push nothing (actually the same as opNull())
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  protected Lattice transferInitialization(IRNode node, Lattice value) {
    return super.transferInitialization(node, value);
  }
  
  @Override protected Lattice transferMethodBody(IRNode body, Port kind, Lattice value) {
    Store s = (Store) value;
    if (kind instanceof EntryPort) {
      return s.opStart();
    } else {
      final boolean fineIsLoggable = LOG.isLoggable(Level.FINE);
      IRNode mdecl = tree.getParent(body);
      String name = "<unknown>";
      if (tree.getOperator(mdecl) instanceof MethodDeclaration
        || tree.getOperator(mdecl) instanceof ConstructorDeclaration) {
        name = JJNode.getInfo(mdecl);
      }
      if (kind instanceof NormalExitPort
        && tree.getOperator(mdecl) instanceof MethodDeclaration
        && !(tree.getOperator(MethodDeclaration.getReturnType(mdecl))
          instanceof VoidType)) {
        IRNode rnode = JavaPromise.getReturnNode(mdecl);
        s = s.opGet(rnode);
        if (UniquenessRules.isUnique(rnode)) {
          s = s.opUndefine();
        } else {
          s = s.opCompromise();
        }
        if (fineIsLoggable) {
          LOG.fine("After handling return value of " + name + ": " + s);
        }
      }
      s = s.opStop();
      if (fineIsLoggable) {
        LOG.fine("At end of " + name + ": " + s);
      }
      return s;
    }
  }

  @Override protected Lattice transferReturn(IRNode node, Lattice val) {
    Store s = (Store) val;
    while (node != null
      && !MethodDeclaration.prototype.includes(tree.getOperator(node))) {
      node = tree.getParentOrNull(node);
    }
    if (node == null) {
      return pop(s);
    } else {
      return s.opSet(JavaPromise.getReturnNode(node));
    }
  }

  @Override protected Lattice transferThrow(IRNode node, Lattice val) {
    Store s = (Store) val;
    return s.opCompromise();
  }

  @Override protected Lattice transferUseArray(IRNode aref, Lattice value) {
    Store s = (Store) value;
    if (!s.isValid())
      return s;
    if (isBothLhsRhs(aref)) {
      s = (Store) dup(dup(s));
    }
    // LOG.fine("Before 2 pops and one push: " + s);
    // for now, treat as a shared field:
    return push(pop(pop(s)));
  }

  @Override protected Lattice transferUseField(IRNode fref, Lattice val) {
    Store s = (Store) val;
    if (!s.isValid())
      return s;
    if (isBothLhsRhs(fref)) {
      s = (Store) dup(s);
    }
    IRNode decl = binder.getBinding(fref);
    if (decl == null) {
      LOG.warning("No binding for field ref " + DebugUnparser.toString(fref));
      s = s.pop();
      return push(s);
    } else {
      return s.opLoad(decl);
    }
  }
  
  @Override protected Lattice transferUseArrayLength(IRNode alen, Lattice val) {
    Store s = (Store) val;
    if (!s.isValid())
      return s;
    
    s = s.pop();
    return s.push(); // push a non pointer value (a primitive integer in this case)
  }
  

  @Override protected Lattice transferUseVar(IRNode var, Lattice value) {
    Store s = (Store) value;
    IRNode decl = binder.getBinding(var);
    if (decl == null) {
      LOG.warning("Cannot find binding for " + DebugUnparser.toString(var));
      return push(s);
    } else {
      return s.opGet(decl);
    }
  }
}

/** Analysis of a special pieces by diurectly calling the transfer function..
 * TODO:
 * Unfortunately, we need an array type (from type environment) and
 * the promises associated with Object and array, and we can't get it
 * until we have a shared VIC for arrays and other JDK stuff, as well as
 * determine what the canonical form for promises is.  These hard problems
 * haven't been solved.
 */
class TestUniqueTransfer {
  static IRNode root = new PlainIRNode();
  static FakeBinder fb = new FakeBinder(root);
  static UniqueTransfer ut = 
    new UniqueTransfer(null, null, fb,
        new Effects(fb, new BindingContextAnalysis(fb)));

  public static void main(String[] args) {
	  initRoot();
    if (args.length == 0 || args[0].equals("aaron-test-1")) {
      aarontest1();
    } else if (args[0].equals("field-init")) {
      field_init_test(true, true);
      field_init_test(true, false);
      field_init_test(false, true);
      field_init_test(false, false);
    } else if (args[0].equals("array-op-equal")) {
      array_op_equal_test(false);
      array_op_equal_test(true);
    } else {
      System.err.println("Unknown test " + args[0]);
    }
  }
  
	private static void initRoot() {
		Era e = new Era(Version.getInitialVersion());
		Version.bumpVersion();
		Version.setDefaultEra(e);
		JJNode.tree.initNode(root, null, 0);
		JJNode.tree.clearParent(root);
	}


  static void aarontest1() {
    IRNode sharedField = VariableDeclarator.createNode("o1", 0, null);
    IRNode recDecl = ReceiverDeclaration.prototype.createNode();
    UniquenessRules.setIsBorrowed(recDecl, true);
    IRNode luse = ThisExpression.prototype.createNode();
    IRNode ruse = ThisExpression.prototype.createNode();
    IRNode lval = FieldRef.createNode(luse, "o1");
    IRNode assign = AssignExpression.createNode(lval, ruse);

    JJNode.tree.appendChild(root, assign);
    JJNode.tree.appendChild(root, sharedField);

    fb.setBinding(lval, sharedField);
    fb.setBinding(luse, recDecl);
    fb.setBinding(ruse, recDecl);

    Store s = new Store(new IRNode[] { recDecl });

    System.out.println("{");
    s = s.opStart();
    System.out.println(s);

    Lattice val = s;

    System.out.println("  this");
    val = ut.transferUseVar(luse, val);
    System.out.println(val);

    System.out.println("  .o1 = // no evaluation yet");
    System.out.println("  this // first just use this");
    val = ut.transferUseVar(luse, val);
    System.out.println(val);

    System.out.println("  ; // now perform assignment");
    val = ut.transferAssignField(lval, val);
    System.out.println(val);
  }

  static void field_init_test(boolean isStatic, boolean isUnique) {
    IRNode sharedValue = StringLiteral.createNode("hello");
    IRNode field =
      VariableDeclarator.createNode(
        isUnique ? "unique" : "shared",
        0,
        Initialization.createNode(sharedValue));
    if (isStatic) {
      JavaNode.setModifiers(field, JavaNode.STATIC);
    }
    if (isUnique) {
      UniquenessRules.setIsUnique(field, true);
    }
    IRNode fields = VariableDeclarators.createNode(new IRNode[] { field });
    IRNode fieldDecl =
      FieldDeclaration.createNode(Annotations.createNode(noNodes),
        isStatic ? JavaNode.STATIC : 0,
        NamedType.createNode("Object"),
        fields);

    IRNode recDecl = ReceiverDeclaration.prototype.createNode();
    UniquenessRules.setIsBorrowed(recDecl, true);

    IRNode unusedDecl = VariableDeclarator.createNode("unused", 0, null);

    JJNode.tree.appendChild(root, fieldDecl);
    JJNode.tree.appendChild(root, unusedDecl);

    Store s = new Store(new IRNode[] { isStatic ? unusedDecl : recDecl });

    if (isStatic)
      System.out.print("static ");
    System.out.println("{");
    s = s.opStart();
    System.out.println(s);

    Lattice val = s;

    System.out.println(
      "  Object "
        + (isUnique ? "unique" : "shared")
        + " = \"hello\" // first evaluate string literal");
    val = ut.transferLiteral(sharedValue, val);
    System.out.println(val);

    System.out.println("  // now perform initialization");
    val = ut.transferInitialization(field, val);
    System.out.println(val);
  }

  static void array_op_equal_test(boolean use_field) {
    System.out.println(
      "\nChecking ?[1] += 2 for ? = " + (use_field ? "this.f" : "a"));
    IRNode field =
      VariableDeclarator.createNode(
        "f",
        1,
        NoInitialization.prototype.createNode());
    IRNode fields = VariableDeclarators.createNode(new IRNode[] { field });
    IRNode fieldDecl =
      FieldDeclaration.createNode(Annotations.createNode(noNodes),
                                  0, IntType.prototype.createNode(), fields);
    IRNode luse = ThisExpression.prototype.createNode();
    IRNode fuse = FieldRef.createNode(luse, "f");

    IRNode intType = IntType.prototype.createNode();
    IRNode arrayType = ArrayType.createNode(intType, 1);
    IRNode arrayParam =
      ParameterDeclaration.createNode(Annotations.createNode(noNodes),
                                      0, arrayType, "a");
    IRNode recDecl = ReceiverDeclaration.prototype.createNode();
    UniquenessRules.setIsBorrowed(recDecl, true);
    IRNode ause = VariableUseExpression.createNode("a");

    IRNode int1 = IntLiteral.createNode("1");
    IRNode int2 = IntLiteral.createNode("2");
    IRNode aref =
      ArrayRefExpression.createNode(use_field ? fuse : ause, int1);
    IRNode opassign =
      OpAssignExpression.createNode(
        aref,
        AddExpression.prototype,
        int2);

    JJNode.tree.appendChild(root,fieldDecl);
    JJNode.tree.appendChild(root, arrayParam);
    JJNode.tree.appendChild(root, opassign);

    fb.setBinding(ause, arrayParam);
    fb.setBinding(luse, recDecl);
    fb.setBinding(fuse, field);

    Store s = new Store(new IRNode[] { recDecl, arrayParam });

    System.out.println("{");
    s = s.opStart();
    System.out.println(s);

    Lattice val = s;

    if (use_field) {
      System.out.println("  this");
      val = ut.transferComponentFlow(luse, null, val);
      System.out.println(val);

      System.out.println("  <check this>");
      val = ut.transferComponentChoice(fuse, null, true, val);
      System.out.println(val);

      System.out.println("  this.f");
      val = ut.transferComponentFlow(fuse, null, val);
      System.out.println(val);
    } else {
      System.out.println("  a");
      val = ut.transferComponentFlow(ause, null, val);
      System.out.println(val);
    }

    System.out.println("  1");
    val = ut.transferComponentFlow(int1, null, val);
    System.out.println(val);

    System.out.println("  <check array>");
    val = ut.transferComponentChoice(aref, "array", true, val);
    System.out.println(val);

    System.out.println("  <check index>");
    val = ut.transferComponentChoice(aref, "index", true, val);
    System.out.println(val);

    System.out.println("  ?[1]");
    val = ut.transferComponentFlow(aref, null, val);
    System.out.println(val);

    System.out.println("  2");
    val = ut.transferComponentFlow(int2, null, val);
    System.out.println(val);

    System.out.println("  ?[1] + 2");
    val = ut.transferComponentChoice(opassign, Boolean.TRUE, true, val);
    System.out.println(val);

    System.out.println("  ?[1] += 2");
    val = ut.transferComponentChoice(opassign, null, true, val);
    System.out.println(val);
  }
}

class FixBinder extends AbstractBinder {
  /** Logger instance for debugging. */
  private static final Logger LOG = SLLogger.getLogger("FLUID.analysis.unique");

  public final IBinder binder;
  public FixBinder(IBinder b) {
    binder = b;
  }
 
  public IBinding getIBinding(IRNode node) {
    IBinding result;
    try {
      result = binder.getIBinding(node);
    } catch (FluidError ex) {
      LOG.log(
        Level.SEVERE,
        "Binder.getBinding crashed on " + DebugUnparser.toString(node),
        ex);
      return null;
    } catch (NullPointerException ex) {
      LOG.log(
        Level.SEVERE,
        "Binder.getBinding crashed on " + DebugUnparser.toString(node),
        ex);
      return null;
    }
    if (LOG.isLoggable(Level.FINE) && result == null) {
      LOG.fine("(no binding for " + DebugUnparser.toString(node) + ")");
    }
    return result;
  }
//  @Override public IRNode getType(IRNode node) {
//    try {
//      return binder.getType(node);
//    } catch (FluidError ex) {
//      LOG.log(
//        Level.SEVERE,
//        "Binder.getType crashed on " + DebugUnparser.toString(node),
//        ex);
//      return null;
//    }
//  }
  @Override public IJavaType getJavaType(IRNode node) {
    try {
      return binder.getJavaType(node);
    } catch (FluidError ex) {
      LOG.log(
          Level.SEVERE,
          "Binder.getType crashed on " + DebugUnparser.toString(node),
          ex);
      return null;
    }
  }
  
  @Override public IRNode getRegionParent(IRNode region) {
    return binder.getRegionParent(region);
  }
  @Override public ITypeEnvironment getTypeEnvironment() {
    return binder.getTypeEnvironment();
  }

  @Override public IRNode findRegionInType(IRNode type, String region) {
    // JTB: NB to self: don't edit FixBinder for Fluid-only operations.
    return binder.findRegionInType(type, region);
  }

  @Override public Object findClassBodyMembers(IRNode type, ISuperTypeSearchStrategy tvs, boolean throwIfNotFound) {
    return binder.findClassBodyMembers(type, tvs, throwIfNotFound);
  }

	@Override public Iteratable<IRNode> findOverriddenMethods(IRNode methodDeclaration) {
		return binder.findOverriddenMethods(methodDeclaration);
	}
}

/** Analysis of a hard-coded parse tree so we can trace everything.
 * TODO:
 * Unfortunately, we need an array type (from type environment) and
 * the promises associated with Object and array, and we can't get it
 * until we have a shared VIC for arrays and other JDK stuff, as well as
 * determine what the canonical form for promises is.  These hard problems
 * haven't been solved.
 */
class TestUniqueAnalysis {
  static IRNode root = new PlainIRNode();
  
  static FakeBinder fb = new FakeBinder(root);
  static UniqueAnalysis ua = 
    new UniqueAnalysis(fb, new Effects(fb, new BindingContextAnalysis(fb)));

  public void reportError(String msg) {
    System.out.println(msg);
  }
  public static void main(String[] args) {
    new TestUniqueAnalysis().test(args);
  }
  /**
   * @param args
   */
  void test(String[] args) {
		initRoot();
		for (int i = 0; i < args.length; ++i) {
			System.out.println("\n----------------------------------------------");
			System.out.println("\n  TEST: " + args[i] + "\n");
			testTree(makeTree(args[i]));
		}
	}

	private static void initRoot() {
		Era e = new Era(Version.getInitialVersion());
		Version.bumpVersion();
		Version.setDefaultEra(e);
		JJNode.tree.initNode(root, null, 0);
		JJNode.tree.clearParent(root);
	}

  static void testTree(IRNode subtree) {
    TestEvaluationTransfer.runAll(subtree);
    java.util.Set<IRNode> seenFlowUnit = new java.util.HashSet<IRNode>();
    JJNode.tree.addChild(root, subtree);

    for (Iterator<IRNode> nodes = JJNode.tree.bottomUp(subtree);
      nodes.hasNext();
      ) {
      IRNode node = nodes.next();
      System.out.println(">> " + DebugUnparser.toString(node) + " <<");
      if (Initializer.prototype.includes(JJNode.tree.getOperator(node))) {
        Lattice b = ua.getAnalysisResultsBefore(node, null);
        IRNode fu = IntraproceduralAnalysis.getFlowUnit(node);
        FlowUnit fuo = (FlowUnit) JJNode.tree.getOperator(fu);
        System.out.println("Flow unit = " + DebugUnparser.toString(fu));
        if (!seenFlowUnit.contains(fu)) {
          edu.cmu.cs.fluid.control.Test.traverse(fuo.getSource(fu));
          seenFlowUnit.add(fu);
        }
        System.out.println("For node: " + DebugUnparser.toString(node));
        System.out.println("before " + b);
        System.out.println("after " + ua.getAnalysisResultsAfter(node, null));
        System.out.println("abrupt " + ua.getAnalysisResultsAbrupt(node, null));
      }
    }
  }

  static IRNode makeBadFieldInitTree() {
    IRNode unique_field;
    IRNode cd, te;
    IRNode result =
      ClassDeclaration
        .createNode(Annotations.createNode(noNodes),
          JavaNode.PUBLIC,
          "Foo",
          TypeFormals.createNode(new IRNode[0]),
          NamedType.createNode("java.lang.Object"),
          Implements.createNode(new IRNode[] {
    }),
      ClassBody
        .createNode(new IRNode[] {
          FieldDeclaration.createNode(Annotations.createNode(noNodes),
            JavaNode.PRIVATE,
            NamedType.createNode("java.lang.Object"),
            VariableDeclarators.createNode(
              new IRNode[] {
                unique_field =
                  VariableDeclarator.createNode(
                    "o",
                    0,
                    Initialization.createNode(
                      StringLiteral.createNode("shared")))})),
          cd =
            ConstructorDeclaration
              .createNode(Annotations.createNode(noNodes),
                JavaNode.PUBLIC,
                TypeFormals.createNode(new IRNode[0]),
                "Foo",
                Parameters.createNode(new IRNode[] {
      }), Throws.createNode(new IRNode[] {
      }),
        MethodBody
          .createNode(
            BlockStatement
            .createNode(new IRNode[] {
              ExprStatement.createNode(
              NonPolymorphicConstructorCall
              .createNode(
                SuperExpression.prototype.createNode(),
                Arguments.createNode(new IRNode[] {
        }))),
          ExprStatement.createNode(
            AssignExpression.createNode(
              FieldRef.createNode(
                te = ThisExpression.prototype.createNode(),
                "o"),
              NullLiteral.prototype.createNode()))
        })))
        }));
    UniquenessRules.setIsUnique(unique_field, true);
    IRNode rec_decl = ReceiverDeclaration.getReceiverNode(cd);
    fb.setBinding(te, rec_decl);
    return result;
  }

  static IRNode makeTree(String kind) {
    if (kind.equals("bad-field-init")) {
      return makeBadFieldInitTree();
    }
    System.err.println("! No such test available");
    System.exit(100);
    return null;
  }
}
