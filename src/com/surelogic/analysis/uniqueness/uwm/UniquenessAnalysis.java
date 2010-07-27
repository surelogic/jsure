package com.surelogic.analysis.uniqueness.uwm;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.analysis.effects.Effect;
import com.surelogic.analysis.effects.Effects;
import com.surelogic.analysis.effects.targets.Target;
import com.surelogic.analysis.regions.IRegion;
import com.surelogic.analysis.uniqueness.uwm.store.State;
import com.surelogic.analysis.uniqueness.uwm.store.Store;
import com.surelogic.analysis.uniqueness.uwm.store.StoreLattice;
import com.surelogic.annotation.rules.UniquenessRules;
import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.FluidError;
import edu.cmu.cs.fluid.control.EntryPort;
import edu.cmu.cs.fluid.control.NormalExitPort;
import edu.cmu.cs.fluid.control.Port;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaArrayType;
import edu.cmu.cs.fluid.java.bind.IJavaDeclaredType;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.operator.AnonClassExpression;
import edu.cmu.cs.fluid.java.operator.BlockStatement;
import edu.cmu.cs.fluid.java.operator.CallInterface;
import edu.cmu.cs.fluid.java.operator.ClassBody;
import edu.cmu.cs.fluid.java.operator.ClassDeclaration;
import edu.cmu.cs.fluid.java.operator.ConstructorDeclaration;
import edu.cmu.cs.fluid.java.operator.DeclStatement;
import edu.cmu.cs.fluid.java.operator.MethodCall;
import edu.cmu.cs.fluid.java.operator.MethodDeclaration;
import edu.cmu.cs.fluid.java.operator.NullLiteral;
import edu.cmu.cs.fluid.java.operator.RefLiteral;
import edu.cmu.cs.fluid.java.operator.ReferenceType;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.operator.VariableDeclarators;
import edu.cmu.cs.fluid.java.operator.VariableUseExpression;
import edu.cmu.cs.fluid.java.operator.VoidType;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;
import edu.uwm.cs.fluid.java.control.JavaEvaluationTransfer;
import edu.uwm.cs.fluid.java.control.SubAnalysisFactory;

public final class UniquenessAnalysis {
  private static final class UniquenessTransfer extends JavaEvaluationTransfer<StoreLattice, Store> {
    /** Logger instance for debugging. */
    private static final Logger LOG = SLLogger
        .getLogger("FLUID.analysis.unique.transfer");

    private final Effects effects;

    private final IRNode flowUnit;

    
    
    // ==================================================================
    // === Constructor 
    // ==================================================================

    public UniquenessTransfer(final IBinder binder, final StoreLattice lattice,
        final SubAnalysisFactory<StoreLattice, Store> factory, final int floor,
        final IRNode fu, final Effects e) {
      super(binder, lattice, factory, floor);
      flowUnit = fu;
      effects = e;
    }

    
    
    // ==================================================================
    // === Methods from JavaEvaluationTransfer 
    // ==================================================================

    /** Pop and discard value from top of stack */
    @Override
    protected Store pop(final Store s) {
      return lattice.pop(s);
    }

    /** Push an unknown shared value onto stack. */
    @Override
    protected Store push(final Store s) {
      return lattice.opExisting(s, State.SHARED);
    }
    
    /** Remove the second from top value from stack */
    @Override
    protected Store popSecond(final Store s) {
      if (!s.isValid()) return s;
      return lattice.opSet(s, lattice.getUnderTop(s));
    }

    @Override
    protected Store dup(final Store s) {
      if (!s.isValid()) return s;
      return lattice.opGet(s, lattice.getStackTop(s));
    }
    
    /** Remove all pending values from stack */
    @Override
    protected Store popAllPending(Store s) {
      while (s.isValid() && lattice.getStackTop(s).intValue() > 0) {
        s = lattice.pop(s);
      }
      return s;
    }

    
    
    // ==================================================================
    // === Other Helper Methods 
    // ==================================================================
    /**
     * Return a store after taking into account these effects (usually inferred
     * from a method call).
     */
    private Store considerEffects(final IRNode rcvr, final IRNode actuals,
        final Set<Effect> effects, Store s) {
      final int n = tree.numChildren(actuals);
      for (final Effect f : effects) {
        if (f.isEmpty()) {
          // empty effects are harmless
          continue;
        }
        
        if (f.isRead()) {
          // case 1: using permissions:
          // we only can bury aliases if we have write permission,
          // so we can ignore this case.
          // but with alias burying, we cannot ignore reads
          // CAN'T: continue;
        }
        
        final Target t = f.getTarget();
        final IRNode ref = t.getReference();
        
        if (ref != null) {
          // case 2:
          // if we are referencing a parameter or receiver, it may be
          // it is a final class with no unique fields.
          final IJavaType ty = binder.getJavaType(ref);
          if (ty instanceof IJavaArrayType) {
            // case 2a: if a "unique Array", then problems.
            // otherwise done
            // ! No unique array types (I think) ?
            continue;
          }
          if (ty instanceof IJavaDeclaredType) {
            final IRNode cd = ((IJavaDeclaredType) ty).getDeclaration();

            if (cd != null
                && ClassDeclaration.prototype.includes(cd)
                && JavaNode.getModifier(cd, JavaNode.FINAL)) {
              final boolean hasUnique = regionHasUniqueFieldInClass(t.getRegion(), cd);
              if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("Effect " + f + " is on final class with"
                    + (hasUnique ? "" : "out") + " unique fields");
              }
              if (!hasUnique)
                continue;
            }
          }
        }
        
        // From this point assume we are going to negate everything
        // reachable from the target.

        // First we load the reference of the target
        if (ref == null) {
          s = lattice.opExisting(s, State.SHARED);
        } else if (ref.equals(rcvr)) {
          s = lattice.opDup(s, n);
        } else {
          foundActual: {
            for (int i = 0; i < n; ++i) {
              if (tree.getChild(actuals, i).equals(ref)) {
                s = lattice.opDup(s, n - i + 1);
                break foundActual;
              }
            }
            s = lattice.opExisting(s, State.SHARED);
          }
        }
        
        final IRegion r = t.getRegion();
        if (r.isAbstract()) {
          // it could refer to almost anything
          // everything reachable from this pointer is alias buried:
          s = lattice.opLoadReachable(s);
        } else {
          // it's a field, load and discard.
          s = lattice.pop(lattice.opLoad(s, r.getNode()));
        }
      }
      return s;
    }

    /**
     * Return true if the given region may have a field in the given class that is
     * unique.
     */
    private boolean regionHasUniqueFieldInClass(IRegion reg, IRNode cd) {
      // TODO define this method
      //
      // One possibility is to enumerate the fields of the class
      // and ask each one if they are in the region.
      //
      // ! For now, assume the worst:
      return true;
    }

    /**
     * Return the store after popping off and processing each actual parameter
     * according to the formal parameters. The number should be the same (or else
     * how did the binder determine to call this method/constructor?).
     */
    private Store popArguments(
        final IRNode actuals, final IRNode formals, Store s) {
      int n = tree.numChildren(actuals);
      if (formals != null && n != tree.numChildren(formals)) {
        throw new FluidError("#formals != #actuals");
      }
      while (n-- > 0) {
        final IRNode formal = formals != null ? tree.getChild(formals, n) : null;
        if (formal != null && UniquenessRules.isUnique(formal)) {
          s = lattice.opUndefine(s);
        } else if (formal == null || UniquenessRules.isBorrowed(formal)) {
          s  = lattice.opBorrow(s);
        } else {
          s = lattice.opCompromise(s);
        }
      }
      return s;
    }

    /**
     * The top of the stack was a receiver of a method call, pop it off using one
     * of the three methods for popping depending on the promises/demands about
     * the receiver.
     */
    private Store popReceiver(final IRNode decl, final Store s) {
      if (decl == null) {
        return lattice.opBorrow(s);
      }
      if (JavaNode.getModifier(decl, JavaNode.STATIC)) {
        return lattice.opRelease(s);
      } else {
        final boolean isConstructor = ConstructorDeclaration.prototype.includes(decl);
        final IRNode recDecl = JavaPromise.getReceiverNode(decl);
        final IRNode retDecl = JavaPromise.getReturnNode(decl);
        
        if (UniquenessRules.isUnique(recDecl)) {
          return lattice.opUndefine(s);
        } else if (UniquenessRules.isBorrowed(recDecl) ||
            (isConstructor && UniquenessRules.isUnique(retDecl))) {
          return lattice.opBorrow(s);
        } else {
          if (isConstructor) {
            if (LOG.isLoggable(Level.FINE)) {
              LOG.fine("Receiver is not limited for\n  "
                  + DebugUnparser.toString(decl));
            }
          }
          return lattice.opCompromise(s);
        }
      }
    }

    /**
     * Push an abstract value corresponding to the return decl of the method or
     * constructor declared in decl.
     */
    private Store pushReturnValue(final IRNode decl, final Store s) {
      if (decl == null) {
        // expect the best
        return lattice.opNew(s);
      }
      // XXX: Will crash if given a ConstructorDeclaration
      final IRNode ty = MethodDeclaration.getReturnType(decl);
      if (VoidType.prototype.includes(ty)) {
        return lattice.push(s);
      }
      
      final IRNode retDecl = JavaPromise.getReturnNodeOrNull(decl);
      if (retDecl == null) {
        LOG.severe("Method missing return value declaration");
        return lattice.push(s);
      }
      
      if (UniquenessRules.isUnique(retDecl)) {
        return lattice.opNew(s);
      } else if (ReferenceType.prototype.includes(ty)) {
        return lattice.opExisting(s, State.SHARED);
      } else {
        // non-object
        return lattice.push(s);
      }
    }

    
    
    // ==================================================================
    // === Transfer function methods 
    // ==================================================================

    @Override
    public Store transferAllocation(final IRNode node, final Store s) {
      return lattice.opNew(s);
    }
    
    @Override
    public Store transferAnonClass(final IRNode node, Store s) {
      /* First gather up all variables used in the body and compromise them.
       */
      for (final IRNode n : tree.bottomUp(AnonClassExpression.getBody(node))) {
        if (VariableUseExpression.prototype.includes(n)) {
          final IRNode decl = binder.getBinding(n);
          if (decl == null) {
            LOG.warning("No binding for " + DebugUnparser.toString(node));
          } else {
            // if undefined, then tough, it's an error
            s = lattice.opCompromise(lattice.opGet(s, decl));
          }
        }
      }
      // Now compromise "this" (this is slightly more conservative than necessary)
      s = lattice.opCompromise(lattice.opThis(s));
      return s;
    }
    
    @Override
    public Store transferArrayCreation(final IRNode node, final Store s) {
      // inefficient but simple
      return lattice.opNew(lattice.pop(super.transferArrayCreation(node, s)));
    }
    
    @Override
    public Store transferArrayInitializer(final IRNode node, final Store s) {
      return lattice.opCompromise(s);
    }
    
    @Override
    protected Store transferAssignArray(final IRNode node, final Store s) {
      return lattice.opCompromiseNoRelease(super.transferAssignArray(node, s));
    }
    
    @Override
    protected Store transferAssignField(final IRNode node, Store s) {
      final IRNode fieldDecl = binder.getBinding(node);
      if (fieldDecl == null) {
        LOG.warning("field not bound" + DebugUnparser.toString(node));
        return popSecond(s);
      } else {
        if (!s.isValid()) return s;
        final Integer object = lattice.getUnderTop(s);
        final Integer field = lattice.getStackTop(s);
        // first copy both onto stack
        s = lattice.opGet(lattice.opGet(s, object), field);
        // now perform assignment
        s = lattice.opStore(s, fieldDecl);
        // now pop extraneous object off stack
        return popSecond(s);
      }
    }
    
    @Override
    protected Store transferAssignVar(final IRNode var, final Store s) {
      final IRNode varDecl = binder.getBinding(var);
      if (varDecl == null) {
        LOG.warning("No binding for assigned variable "
            + DebugUnparser.toString(var));
        return s;
      } else {
        if (!s.isValid()) return s;
        return lattice.opSet(dup(s), varDecl);
      }
    }
    
    @Override
    protected Store transferCall(final IRNode node, final boolean flag, Store s) {
      final IRNode mdecl = binder.getBinding(node);
      final Operator op = tree.getOperator(node);
      final boolean mcall = MethodCall.prototype.includes(op);
      
      final CallInterface call = (CallInterface) op;
      final IRNode actuals = call.get_Args(node);
      final IRNode formals;
      if (mdecl == null) {
        LOG.warning("No binding for method " + DebugUnparser.toString(node));
        formals = null;
      } else {
        if (ConstructorDeclaration.prototype.includes(mdecl)) {
          formals = ConstructorDeclaration.getParams(mdecl);
        } else {
          formals = MethodDeclaration.getParams(mdecl);
        }
      }
      final IRNode receiverNode = mcall ? ((MethodCall) call).get_Object(node) : null;
      if (mdecl != null) {
        /* If the flowunit is a class body then we are dealing with instance 
         * initialization and the calls is the <init> method.
         */
        final IRNode caller;
        if (ClassBody.prototype.includes(flowUnit)) {
          caller = JavaPromise.getInitMethod(JJNode.tree.getParent(flowUnit));
        } else {
          caller = flowUnit;
        }
        s = considerEffects(receiverNode, actuals,
            effects.getMethodCallEffects(null, node, caller, true), s);
      }
      // We have to possibly compromise arguments
      s = popArguments(actuals, formals, s);
      if (hasOuterObject(node)) {
        if (LOG.isLoggable(Level.FINE)) {
          LOG.fine("Popping qualifiers");
        }
        if (!s.isValid()) return s;
        /* Comproimise value under top: (1) copy it onto top; (2) compromise
         * new top and discard it; (3) popSecond
         */
        s = lattice.opGet(s, lattice.getUnderTop(s));
        s = lattice.opCompromise(s);
        if (!s.isValid()) return s;
        /* This does a popSecond by popping the top value off the stack (op set)
         * and changing the variable just under the top to have the value that 
         * was on the top of the stack.
         */
        s = lattice.opSet(s, lattice.getUnderTop(s));
      }
      if (!mcall) {
        // we keep a copy of the object being constructed
        s = dup(s);
      }
      // for method calls (need return value)
      // for new expressions (object already duplicated)
      // and for constructor calls (also already duplicated)
      // we pop the receiver.
      s = popReceiver(mdecl, s);
      
      if (LOG.isLoggable(Level.FINE)) {
        LOG.fine("After handling receivers/qualifiers/parameter: " + s);
      }
      if (flag) { // call succeeded
        if (mcall) {
          s = pushReturnValue(mdecl, s);
        }
        return s;
      } else {
        // we just pop all pending
        s = popAllPending(s);
        return s;
      }
    }
    
    @Override
    protected Store transferCloseScope(final IRNode node, Store s) {
      /*
       * Nullify all variables that were in scope. NB: "undefined" would be closer
       * in semantics, but this is not done to check errors, but rather for
       * efficiency and null is much cheap in the semantics than undefined. Java's
       * scope rules ensure that we don't.
       */
      for (final IRNode stmt : BlockStatement.getStmtIterator(node)) {
        if (DeclStatement.prototype.includes(stmt)) {
          final IRNode vars = DeclStatement.getVars(stmt);
          for (final IRNode var : VariableDeclarators.getVarIterator(vars)) {
            s = lattice.opNull(s);
            s = lattice.opSet(s, var);
          }
        }
      }
      return s;
    }

    @Override
    protected Store transferDefaultInit(final IRNode node, final Store s) {
      final IRNode ty = VariableDeclarator.getType(tree.getParent(node));
      if (ReferenceType.prototype.includes(ty)) {
        return lattice.opNull(s);
      } else {
        // not an object
        return lattice.push(s);
      }
    }
    
    @Override
    protected Store transferEq(final IRNode node, final boolean flag, final Store s) {
      // compare top two stack values and pop off.
      // then push a boolean value (unused).
      return push(lattice.opEqual(s, flag));
    }
    
    @Override
    protected Store transferFailedCall(final IRNode node, final Store s) {
      throw new FluidError("execution should not reach here.");
    }
    
    @Override
    protected Store transferInitializationOfField(final IRNode node, Store s) {
      final boolean fineIsLoggable = LOG.isLoggable(Level.FINE);
      // get class or "this" on stack
      if (TypeUtil.isStatic(node)) {
        s = lattice.opExisting(s, State.SHARED);
        if (fineIsLoggable) {
          LOG.fine("initializing static field '" + JJNode.getInfo(node) + "'");
        }
      } else {
        s = lattice.opThis(s);
        if (fineIsLoggable) {
          LOG.fine("initializing field '" + JJNode.getInfo(node) + "'");
        }
      }
      s = lattice.opDup(s, 1); // get value on top of stack
      s = lattice.opStore(s, node); // perform store
      s = lattice.opRelease(s); // throw away the extra copy
      return s;
    }
    
    @Override
    protected Store transferInitializationOfVar(final IRNode node, final Store s) {
      return lattice.opSet(s, node);
    }
    
    @Override
    protected Store transferLiteral(final IRNode node, final Store s) {
      final Operator op = tree.getOperator(node);
      if (NullLiteral.prototype.includes(op)) {
        return lattice.opNull(s);
      } else if (RefLiteral.prototype.includes(op)) {
        return push(s); // push a shared String constant
      } else {
        return lattice.push(s); // push nothing (actually the same as opNull());
      }
    }
    
    /* Original override transferIntialization(), but there is no reason to. */
    
    @Override
    protected Store transferMethodBody(final IRNode body, final Port kind, Store s) {
      if (kind instanceof EntryPort) {
        return lattice.opStart(s);
      } else {
        final boolean fineIsLoggable = LOG.isLoggable(Level.FINE);
        final IRNode mdecl = tree.getParent(body);
        final Operator mOp = tree.getOperator(mdecl);
        String name = "<unknown>";
        if (MethodDeclaration.prototype.includes(mOp) ||
            ConstructorDeclaration.prototype.includes(mOp)) {
          name = JJNode.getInfo(mdecl);
        }
        if (kind instanceof NormalExitPort &&
            MethodDeclaration.prototype.includes(mOp) && 
            !VoidType.prototype.includes(MethodDeclaration.getReturnType(mdecl))) {
          final IRNode rnode = JavaPromise.getReturnNode(mdecl);
          s = lattice.opSet(s, rnode);
          if (UniquenessRules.isUnique(rnode)) {
            s = lattice.opUndefine(s);
          } else {
            s = lattice.opCompromise(s);
          }
          if (fineIsLoggable) {
            LOG.fine("After handling return value of " + name + ": " + s);
          }
        }
        s = lattice.opStop(s);
        if (fineIsLoggable) {
          LOG.fine("At end of " + name + ": " + s);
        }
        return s;
      }
    }
    
    @Override
    protected Store transferReturn(IRNode node, final Store s) {
      while (node != null && !MethodDeclaration.prototype.includes(node)) {
        node = tree.getParentOrNull(node);
      }
      if (node == null) {
        return pop(s);
      } else {
        return lattice.opSet(s, JavaPromise.getReturnNode(node));
      }
    }
    
    @Override
    protected Store transferThrow(final IRNode node, final Store s) {
      return lattice.opCompromise(s);
    }
    
    @Override
    protected Store transferUseArray(final IRNode aref, Store s) {
      if (!s.isValid()) return s;
      if (isBothLhsRhs(aref)) {
        s = dup(dup(s));
      }
      return push(pop(pop(s)));
    }
    
    @Override
    protected Store transferUseField(final IRNode fref, Store s) {
      if (!s.isValid()) return s;
      if (isBothLhsRhs(fref)) {
        s = dup(s);
      }
      final IRNode decl = binder.getBinding(fref);
      if (decl == null) {
        LOG.warning("No binding for field ref " + DebugUnparser.toString(fref));
        s = lattice.pop(s);
        return push(s);
      } else {
        return lattice.opLoad(s, decl);
      }
    }
    
    @Override
    protected Store transferUseArrayLength(final IRNode alen, Store s) {
      if (!s.isValid()) return s;
      s = lattice.pop(s);
      return lattice.push(s); // push a non-pointer value (a primitive integer in this case)
    }
    
    @Override
    protected Store transferUseVar(final IRNode var, final Store s) {
      final IRNode decl = binder.getBinding(var);
      if (decl == null) {
        LOG.warning("Cannot find binding for " + DebugUnparser.toString(var));
        return push(s);
      } else {
        return lattice.opGet(s, decl);
      }
    }
    
    
    
    public Store transferComponentSource(final IRNode node) {
      // XXX: is this right?
      return lattice.bottom();
    }
  }
}
