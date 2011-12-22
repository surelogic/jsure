package com.surelogic.analysis.uniqueness.sideeffecting;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.analysis.AbstractWholeIRAnalysis;
import com.surelogic.analysis.IBinderClient;
import com.surelogic.analysis.JavaSemanticsVisitor;
import com.surelogic.analysis.LocalVariableDeclarations;
import com.surelogic.analysis.alias.IMayAlias;
import com.surelogic.analysis.alias.TypeBasedMayAlias;
import com.surelogic.analysis.bca.BindingContextAnalysis;
import com.surelogic.analysis.effects.Effect;
import com.surelogic.analysis.effects.Effects;
import com.surelogic.analysis.effects.targets.Target;
import com.surelogic.analysis.regions.IRegion;
import com.surelogic.analysis.uniqueness.sideeffecting.store.State;
import com.surelogic.analysis.uniqueness.sideeffecting.store.Store;
import com.surelogic.analysis.uniqueness.sideeffecting.store.StoreLattice;
import com.surelogic.analysis.uniqueness.sideeffecting.store.StoreLattice.MessageChooser;
import com.surelogic.annotation.rules.MethodEffectsRules;
import com.surelogic.annotation.rules.UniquenessRules;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.util.IThunk;

import edu.cmu.cs.fluid.FluidError;
import edu.cmu.cs.fluid.control.AbruptExitPort;
import edu.cmu.cs.fluid.control.EntryPort;
import edu.cmu.cs.fluid.control.NormalExitPort;
import edu.cmu.cs.fluid.control.Port;
import edu.cmu.cs.fluid.control.Component.WhichPort;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.analysis.AbstractJavaFlowAnalysisQuery;
import edu.cmu.cs.fluid.java.analysis.SimplifiedJavaFlowAnalysisQuery;
import edu.cmu.cs.fluid.java.bind.AbstractBinder;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IBinding;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.bind.ISuperTypeSearchStrategy;
import edu.cmu.cs.fluid.java.bind.ITypeEnvironment;
import edu.cmu.cs.fluid.java.operator.AnnotationElement;
import edu.cmu.cs.fluid.java.operator.AnonClassExpression;
import edu.cmu.cs.fluid.java.operator.ArithUnopExpression;
import edu.cmu.cs.fluid.java.operator.BlockStatement;
import edu.cmu.cs.fluid.java.operator.CallInterface;
import edu.cmu.cs.fluid.java.operator.CatchClause;
import edu.cmu.cs.fluid.java.operator.ClassBody;
import edu.cmu.cs.fluid.java.operator.ClassInitializer;
import edu.cmu.cs.fluid.java.operator.CompareExpression;
import edu.cmu.cs.fluid.java.operator.ComplementExpression;
import edu.cmu.cs.fluid.java.operator.ConstructorDeclaration;
import edu.cmu.cs.fluid.java.operator.DeclStatement;
import edu.cmu.cs.fluid.java.operator.EnumConstantClassDeclaration;
import edu.cmu.cs.fluid.java.operator.EqualityExpression;
import edu.cmu.cs.fluid.java.operator.FieldDeclaration;
import edu.cmu.cs.fluid.java.operator.InstanceOfExpression;
import edu.cmu.cs.fluid.java.operator.MethodCall;
import edu.cmu.cs.fluid.java.operator.MethodDeclaration;
import edu.cmu.cs.fluid.java.operator.NullLiteral;
import edu.cmu.cs.fluid.java.operator.RefLiteral;
import edu.cmu.cs.fluid.java.operator.ReferenceType;
import edu.cmu.cs.fluid.java.operator.SomeFunctionDeclaration;
import edu.cmu.cs.fluid.java.operator.StringConcat;
import edu.cmu.cs.fluid.java.operator.UnboxExpression;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.operator.VariableDeclarators;
import edu.cmu.cs.fluid.java.operator.VariableUseExpression;
import edu.cmu.cs.fluid.java.operator.VoidType;
import edu.cmu.cs.fluid.java.promise.ReceiverDeclaration;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.drops.effects.RegionEffectsPromiseDrop;
import edu.cmu.cs.fluid.sea.drops.promises.BorrowedPromiseDrop;
import edu.cmu.cs.fluid.sea.drops.promises.UniquePromiseDrop;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.Iteratable;
import edu.uwm.cs.fluid.java.analysis.IntraproceduralAnalysis;
import edu.uwm.cs.fluid.java.control.AbstractCachingSubAnalysisFactory;
import edu.uwm.cs.fluid.java.control.IJavaFlowAnalysis;
import edu.uwm.cs.fluid.java.control.JavaEvaluationTransferSE;
import edu.uwm.cs.fluid.java.control.JavaForwardAnalysis;

public final class UniquenessAnalysis extends IntraproceduralAnalysis<Store, StoreLattice, JavaForwardAnalysis<Store, StoreLattice>> implements IBinderClient {
  // ==================================================================
  // === Fields
  // ==================================================================

  private final boolean timeOut;
  private final IMayAlias mayAlias;

  private final BindingContextAnalysis bindingContext;
  

  // for creating drops
  private final AbstractWholeIRAnalysis<UniquenessAnalysis,Void> analysis;
  
  
  // ==================================================================
  // === Constructor 
  // ==================================================================
  
  public UniquenessAnalysis(
      final AbstractWholeIRAnalysis<UniquenessAnalysis,Void> a,
      final IBinder binder, final boolean to,
      final BindingContextAnalysis bca) {
    super(new FixBinder(binder)); // avoid crashes.
    mayAlias = new TypeBasedMayAlias(binder);
    analysis = a;
    timeOut = to;
    bindingContext = bca;
  }
  
  
  
  // ==================================================================
  // === Methods from IntraproceduralAnalysis
  // ==================================================================

  @Override
  protected JavaForwardAnalysis<Store, StoreLattice> createAnalysis(
      final IRNode flowUnit) {
    /* Get all the local variables with reference types, including final
     * variables declared in outer scopes, the "return variable" and the
     * receiver.
     */
    final LocalVariableDeclarations lvd = LocalVariableDeclarations.getDeclarationsFor(flowUnit);
    final List<IRNode> refLocals = new ArrayList<IRNode>();
    final List<IRNode> trash = new ArrayList<IRNode>();
    LocalVariableDeclarations.separateDeclarations(binder, lvd.getLocal(), refLocals, trash);
    LocalVariableDeclarations.separateDeclarations(binder, lvd.getExternal(), refLocals, trash);
    
    // Add the return node
    final IRNode returnNode = JavaPromise.getReturnNodeOrNull(flowUnit);
    if (returnNode != null && LocalVariableDeclarations.hasReferenceType(binder, returnNode)) {
      refLocals.add(returnNode);
    }

    // Add the receiver
    final IRNode rcvrNode = JavaPromise.getReceiverNodeOrNull(flowUnit);
    if (rcvrNode != null) {
      refLocals.add(rcvrNode);
    }

    // Add the qualified receiver, if any.  This will only be present on a
    // constructor.
    final IRNode qrcvrNode = JavaPromise.getQualifiedReceiverNodeOrNull(flowUnit);
    if (qrcvrNode != null) {
    	refLocals.add(qrcvrNode);
    }
        
    /* For each AnonClassExpression that is in the flow unit we add the 
     * receiver from the <init> method  (BUG 1712)
     */
    ReceiverSnatcher.getReceivers(flowUnit, refLocals);
    
    final IRNode[] locals = refLocals.toArray(new IRNode[refLocals.size()]);
    final StoreLattice lattice =
        new StoreLattice(flowUnit, analysis, binder, locals);
    final AtomicBoolean cargo = new AtomicBoolean(false);
    return new Uniqueness(true, cargo, "Uniqueness Analysis (Side Effecting)", lattice,
        new UniquenessTransfer(cargo, binder, mayAlias, lattice,
            bindingContext.getExpressionObjectsQuery(flowUnit),
            0, flowUnit, timeOut),
        timeOut);
  }

  
  
  // ==================================================================
  // === Methods from IBinderClient
  // ==================================================================

  public IBinder getBinder() {
    return binder;
  }

  public void clearCaches() {
    clear();
  }
  
  
  
  // ==================================================================
  // === Wrapper for binder to eat exceptions 
  // ==================================================================

  private static final class FixBinder extends AbstractBinder {
    /** Logger instance for debugging. */
    private static final Logger LOG = SLLogger.getLogger("FLUID.analysis.unique");

    public final IBinder binder;

    public FixBinder(IBinder b) {
      binder = b;
    }

    @Override
    protected IBinding getIBinding_impl(IRNode node) {
      IBinding result;
      try {
        result = binder.getIBinding(node);
      } catch (FluidError ex) {
        LOG.log(Level.SEVERE, "Binder.getBinding crashed on "
            + DebugUnparser.toString(node), ex);
        return null;
      } catch (NullPointerException ex) {
        LOG.log(Level.SEVERE, "Binder.getBinding crashed on "
            + DebugUnparser.toString(node), ex);
        return null;
      }
      if (LOG.isLoggable(Level.FINE) && result == null) {
        LOG.fine("(no binding for " + DebugUnparser.toString(node) + ")");
      }
      return result;
    }

    @Override
    public IJavaType getJavaType(IRNode node) {
      try {
        return binder.getJavaType(node);
      } catch (FluidError ex) {
        LOG.log(Level.SEVERE, "Binder.getType crashed on "
            + DebugUnparser.toString(node), ex);
        return null;
      }
    }

    @Override
    public IRNode getRegionParent(IRNode region) {
      return binder.getRegionParent(region);
    }

    @Override
    public ITypeEnvironment getTypeEnvironment() {
      return binder.getTypeEnvironment();
    }

    @Override
    public IRNode findRegionInType(IRNode type, String region) {
      // JTB: NB to self: don't edit FixBinder for Fluid-only operations.
      return binder.findRegionInType(type, region);
    }

    @Override
    public <T> T findClassBodyMembers(IRNode type, ISuperTypeSearchStrategy<T> tvs,
        boolean throwIfNotFound) {
      return binder.findClassBodyMembers(type, tvs, throwIfNotFound);
    }

    @Override
    public Iteratable<IBinding> findOverriddenMethods(IRNode methodDeclaration) {
      return binder.findOverriddenMethods(methodDeclaration);
    }
  }
  
  
  
  // ==================================================================
  // === Flow Analysis: Subclass to enable side effects
  // ==================================================================

  public static final class Uniqueness extends JavaForwardAnalysis<Store, StoreLattice> {
    private final boolean root;
    private final AtomicBoolean flag;
    
    public Uniqueness(final boolean r, final AtomicBoolean f,
        final String name, final StoreLattice lattice,
        final UniquenessTransfer transfer, boolean timeOut) {
      super(name, lattice, transfer, DebugUnparser.viewer, timeOut);
      root = r;
      flag = f;
    }
    
    @Override
    public void performAnalysis() {
      // Root analysis always starts with side effects turned off.
      realPerformAnalysis();
      if (root) {
        flag.set(true);
        lattice.setSideEffects(true);
      }
      if (flag.get()) {
        reworkAll();
        if (root) {
          lattice.makeResultDrops();
          flag.set(false);
          lattice.setSideEffects(false);
        }
      }
    }
  }

  
  // ==================================================================
  // === Transfer Function 
  // ==================================================================

  private static final class UniquenessTransfer extends JavaEvaluationTransferSE<StoreLattice, Store> {
    /** Logger instance for debugging. */
    private static final Logger LOG = SLLogger
        .getLogger("FLUID.analysis.unique.transfer");

    private final IRNode flowUnit;
    private final IMayAlias mayAlias;
    private final Effects effects;
    private final BindingContextAnalysis.Query bcaQuery;
    
    
    
    // ==================================================================
    // === Constructor 
    // ==================================================================

    public UniquenessTransfer(final AtomicBoolean cargo, 
        final IBinder binder, final IMayAlias ma, final StoreLattice lattice,
        final BindingContextAnalysis.Query query,
        final int floor, final IRNode fu, final boolean timeOut) {
      super(binder, lattice,
          new SubAnalysisFactory(cargo, fu, ma, timeOut, query), floor);
      mayAlias = ma;
      flowUnit = fu;
      effects = new Effects(binder);
      bcaQuery = query;
    } 

    
    
    // ==================================================================
    // === Methods from JavaEvaluationTransfer 
    // ==================================================================

    /** Pop and discard value from top of stack */
    @Override
    protected Store pop(final Store s, final IRNode srcOp) {
      return lattice.pop(s, srcOp);
    }

    /** Push an unknown shared value onto stack. */
    @Override
    protected Store push(final Store s, final IRNode srcOp) {
      return lattice.opExisting(s, srcOp, State.SHARED);
    }
    
    /** Remove the second from top value from stack */
    @Override
    protected Store popSecond(final Store s, final IRNode srcOp) {
      if (!s.isValid()) return s;
      return lattice.opSet(s, srcOp, lattice.getUnderTop(s));
    }

    @Override
    protected Store dup(final Store s, final IRNode srcOp) {
      if (!s.isValid()) return s;
      return lattice.opGet(s, srcOp, lattice.getStackTop(s));
    }
    
    /** Remove all pending values from stack */
    @Override
    protected Store popAllPending(Store s, final IRNode srcOp) {
      while (s.isValid() && lattice.getStackTop(s).intValue() > 0) {
        s = lattice.pop(s, srcOp);
      }
      return s;
    }

    
    
    // ==================================================================
    // === Other Helper Methods 
    // ==================================================================

    private Store considerDeclaredEffects(final IRNode rcvr,
        final int numFormals, final IRNode formals, final IRNode actuals,
        final RegionEffectsPromiseDrop fxDrop,
        final List<Effect> declEffects, Store s, final IRNode srcOp) {
      for (final Effect f : declEffects) {
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
        
        /* Should update this.  Need to look at the types of the 
         * actual parameters for this.
         */
        
//        if (ref != null) {
//          // case 2:
//          // if we are referencing a parameter or receiver, it may be
//          // it is a final class with no unique fields.
//          final IJavaType ty = binder.getJavaType(ref);
//          if (ty instanceof IJavaArrayType) {
//            // case 2a: if a "unique Array", then problems.
//            // otherwise done
//            // ! No unique array types (I think) ?
//            continue;
//          }
//          if (ty instanceof IJavaDeclaredType) {
//            final IRNode cd = ((IJavaDeclaredType) ty).getDeclaration();
//
//            if (cd != null
//                && ClassDeclaration.prototype.includes(cd)
//                && JavaNode.getModifier(cd, JavaNode.FINAL)) {
//              final boolean hasUnique = regionHasUniqueFieldInClass(t.getRegion(), cd);
//              if (LOG.isLoggable(Level.FINE)) {
//                LOG.fine("Effect " + f + " is on final class with"
//                    + (hasUnique ? "" : "out") + " unique fields");
//              }
//              if (!hasUnique)
//                continue;
//            }
//          }
//        }
        
        // From this point assume we are going to negate everything
        // reachable from the target.

        // First we load the reference of the target
        if (ref == null) {
          s = lattice.opExisting(s, srcOp, State.SHARED);
        } else if (ReceiverDeclaration.prototype.includes(ref)) {
          s = lattice.opDup(s, rcvr, numFormals);
        } else {
          foundFormal: {
            for (int i = 0; i < numFormals; ++i) {
              /* If numActuals == 0 we don't get here, so it doesn't matter that
               * actuals == null in that case.
               */
              final IRNode formal = tree.getChild(formals, i);
              if (formal.equals(ref)) {
                s = lattice.opDup(
                    s, tree.getChild(actuals, i), numFormals - i - 1); // was + 1; fixed 2011-01-07
                break foundFormal;
              }
            }
            s = lattice.opExisting(s, srcOp, State.SHARED);
          }
        }
        
        final IRegion r = t.getRegion();
        if (r.isAbstract()) {
          // it could refer to almost anything
          // everything reachable from this pointer is alias buried:
          s = lattice.opLoadReachable(s, srcOp, fxDrop);
        } else {
          // it's a field, load and discard.
          s = lattice.pop(lattice.opLoad(s, srcOp, r.getNode()), srcOp);
        }
      }
      return s;
    }    
    
//    /**
//     * Return true if the given region may have a field in the given class that is
//     * unique.
//     */
//    private boolean regionHasUniqueFieldInClass(IRegion reg, IRNode cd) {
//      // TODO define this method
//      //
//      // One possibility is to enumerate the fields of the class
//      // and ask each one if they are in the region.
//      //
//      // ! For now, assume the worst:
//      return true;
//    }

    /**
     * Return the store after popping off and processing each actual parameter
     * according to the formal parameters. The number should be the same (or else
     * how did the binder determine to call this method/constructor?).
     */
    private Store popArguments(
        final IRNode calledMethod, final IRNode methodCall,
        final int numActuals, final IRNode formals, final IRNode actuals, Store s) {
      if (formals != null && numActuals != tree.numChildren(formals)) {
        throw new FluidError("#formals != #actuals");
      }
      for (int n = numActuals - 1; n >= 0; n--) {
        final IRNode formal = formals != null ? tree.getChild(formals, n) : null;
        final IRNode actual = actuals != null ?  tree.getChild(actuals, n) : null;
        final UniquePromiseDrop uDrop = UniquenessRules.getUnique(formal);
        final BorrowedPromiseDrop bDrop = UniquenessRules.getBorrowed(formal);
        if (uDrop != null) { // used to also check if formal != null
          s = lattice.opUndefine(s, actual, uDrop, MessageChooser.ACTUAL, bcaQuery);
        } else if (/*formal == null ||*/ bDrop != null) {
          s  = lattice.opBorrow(s, actual, calledMethod, methodCall, bDrop, bcaQuery);
        } else {
          s = lattice.opCompromise(s, actual);
        }
      }
      return s;
    }

    /**
     * The top of the stack was a receiver of a method call, pop it off using one
     * of the three methods for popping depending on the promises/demands about
     * the receiver.
     */
    private Store popReceiver(final IRNode decl, final IRNode methodCall, final Store s, final IRNode srcOp) {
//      if (decl == null) {
//        return lattice.opBorrow(s, srcOp);
//      }
      if (JavaNode.getModifier(decl, JavaNode.STATIC)) {
        return lattice.opRelease(s, srcOp);
      } else {
        final boolean isConstructor = ConstructorDeclaration.prototype.includes(decl);
        final IRNode recDecl = JavaPromise.getReceiverNode(decl);
        final IRNode retDecl = JavaPromise.getReturnNode(decl);
        final UniquePromiseDrop uDrop = UniquenessRules.getUnique(recDecl);
        final BorrowedPromiseDrop bDrop = UniquenessRules.getBorrowed(recDecl);
        final UniquePromiseDrop uRetDrop = UniquenessRules.getUnique(retDecl);
        if (uDrop != null) {
          return lattice.opUndefine(s, srcOp, uDrop, MessageChooser.ACTUAL, bcaQuery);
        } else if (bDrop != null) {
          return lattice.opBorrow(s, srcOp, decl, methodCall, bDrop, bcaQuery);
        } else if (isConstructor && uRetDrop != null) {
          return lattice.opBorrow(s, srcOp, decl, methodCall, uRetDrop, bcaQuery);
        } else {
          if (isConstructor) {
            if (LOG.isLoggable(Level.FINE)) {
              LOG.fine("Receiver is not limited for\n  "
                  + DebugUnparser.toString(decl));
            }
          }
          return lattice.opCompromise(s, srcOp);
        }
        
//        else if (UniquenessRules.isBorrowed(recDecl) ||
//            (isConstructor && UniquenessRules.isUnique(retDecl))) {
//          return lattice.opBorrow(s, srcOp);
//        } else {
//          if (isConstructor) {
//            if (LOG.isLoggable(Level.FINE)) {
//              LOG.fine("Receiver is not limited for\n  "
//                  + DebugUnparser.toString(decl));
//            }
//          }
//          return lattice.opCompromise(s, srcOp);
//        }
      }
    }

    /**
     * Push an abstract value corresponding to the return decl of the method or
     * constructor declared in decl.
     */
    private Store pushReturnValue(final IRNode decl, final Store s, final IRNode srcOp) {
      if (decl == null) {
        // expect the best
        return lattice.opNew(s);
      }
      // XXX: Will crash if given a ConstructorDeclaration
      final IRNode ty;
      if (MethodDeclaration.prototype.includes(decl)) {
    	  ty = MethodDeclaration.getReturnType(decl);
    	  
          if (VoidType.prototype.includes(ty)) {
              return lattice.push(s);
          }
      } else {
    	  // No check, since it can't be void
    	  ty = AnnotationElement.getType(decl);
      }
      
      final IRNode retDecl = JavaPromise.getReturnNodeOrNull(decl);
      if (retDecl == null) {
        LOG.severe("Method missing return value declaration");
        return lattice.push(s);
      }
      
      if (UniquenessRules.isUnique(retDecl)) {
        return lattice.opNew(s);
      } else if (ReferenceType.prototype.includes(ty)) {
        return lattice.opExisting(s, srcOp, State.SHARED);
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
      /*
       * Compromise all the variables used in the body of the anonymous class
       * that are externally declared to simulate the fact that they are read
       * and stored in synthetic fields of the anonymous class.
       * 
       * Each one of these variables is visible in the calling context (that is,
       * the flow unit being analyzed) because of Java syntactic nesting rules.
       * Each variable is either declared in the flow unit, or is an external
       * variable visible in the flow unit.
       * 
       * Each initializer/method/constructor of the anonymous class is going to
       * have the same externally declared variables, so we just use the
       * instance initializer to look them up because we know every anonymous
       * class has one.
       */
      final List<IRNode> externalVars = 
        LocalVariableDeclarations.getExternallyDeclaredVariables(
            JavaPromise.getInitMethodOrNull(node));      
      for (final IRNode n : tree.bottomUp(AnonClassExpression.getBody(node))) {
        if (VariableUseExpression.prototype.includes(n)) {
          final IRNode decl = binder.getBinding(n);
          if (externalVars.contains(decl)) {
            s = lattice.opCompromise(lattice.opGet(s, node, decl), node);
          }
        }
      }
      
      // If we aren't in a static context then compromise "this"
      final IRNode rcvr = JavaPromise.getReceiverNodeOrNull(flowUnit);
      if (rcvr != null) { 
        // Now compromise "this" (this is slightly more conservative than necessary)
        s = lattice.opCompromise(lattice.opGet(s, node, rcvr), node);
//        s = lattice.opCompromise(lattice.opThis(s, node), node);
      }
      return s;
    }
    
    @Override
    public Store transferArrayCreation(final IRNode node, final Store s) {
      // inefficient but simple
      return lattice.opNew(lattice.pop(super.transferArrayCreation(node, s), node));
    }
    
    @Override
    public Store transferArrayInitializer(final IRNode node, final Store s) {
      return lattice.opCompromise(s, node);
    }
    
    @Override
    protected Store transferAssignArray(final IRNode node, final Store s) {
      return lattice.opCompromiseNoRelease(super.transferAssignArray(node, s), node);
    }
    
    @Override
    protected Store transferAssignField(final IRNode node, Store s) {
      final IRNode fieldDecl = binder.getBinding(node);
      if (fieldDecl == null) {
        LOG.warning("field not bound" + DebugUnparser.toString(node));
        return popSecond(s, node);
      } else {
        if (!s.isValid()) return s;
        final Integer object = lattice.getUnderTop(s);
        final Integer field = lattice.getStackTop(s);
        // first copy both onto stack
        s = lattice.opGet(lattice.opGet(s, node, object), node, field);
        // now perform assignment
        s = lattice.opStore(s, node, fieldDecl, bcaQuery);
        // now pop extraneous object off stack
        return popSecond(s, node);
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
        return lattice.opSet(dup(s, var), var, varDecl);
      }
    }
    
    @Override
    protected Store transferBinop(IRNode node, Operator op, Store val) {
      if (op instanceof StringConcat)
        return super.transferBinop(node, op, val);
      else {
        return lattice.push(lattice.pop(lattice.pop(val, node), node));
      }
    }

    @Override
    protected Store transferCall(final IRNode node, final boolean isNormal, Store s) {
      final IRNode mdecl = binder.getBinding(node);
      final Operator op = tree.getOperator(node);
      final boolean mcall = MethodCall.prototype.includes(op);
      
      final CallInterface call = (CallInterface) op;
      IRNode actuals;
      int numActuals;
      try {
        actuals = call.get_Args(node);
        numActuals = tree.numChildren(actuals);
      } catch (final CallInterface.NoArgs e) {
        actuals = null;
        numActuals = 0;
      }

      final IRNode formals;
      if (mdecl == null) {
        LOG.warning("No binding for method " + DebugUnparser.toString(node));
        formals = null;
      } else {
        formals = SomeFunctionDeclaration.getParams(mdecl);
//        if (ConstructorDeclaration.prototype.includes(mdecl)) {
//          formals = ConstructorDeclaration.getParams(mdecl);
//        } else if (MethodDeclaration.prototype.includes(mdecl) {
//          formals = MethodDeclaration.getParams(mdecl);
//        } else { // AnnotationElement
//          
//        }
      }
      final IRNode receiverNode = mcall ? ((MethodCall) call).get_Object(node) : null;
      
      if (!isNormal) {
        lattice.setSuppressDrops(true);
      }
      try {
        if (mdecl != null) {
          s = considerDeclaredEffects(
              receiverNode, numActuals, formals, actuals,
              MethodEffectsRules.getRegionEffectsDrop(mdecl),
              effects.getMethodEffects(mdecl, node), s, node);
        }
        // We have to possibly compromise arguments
        s = popArguments(mdecl, node, numActuals, formals, actuals, s);
        final IRNode outerObject = getOuterObject(node);
        if (outerObject != null) {
          if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Popping qualifiers");
          }
          if (!s.isValid()) return s;
          /* Compromise value under top: (1) copy it onto top; (2) compromise
           * new top and discard it; (3) popSecond
           */
          s = lattice.opGet(s, node, lattice.getUnderTop(s));
          s = lattice.opCompromise(s, outerObject);
          if (!s.isValid()) return s;
          /* This does a popSecond by popping the top value off the stack (op set)
           * and changing the variable just under the top to have the value that 
           * was on the top of the stack.
           */
          s = lattice.opSet(s, node, lattice.getUnderTop(s));
        }
        if (!mcall) {
          // we keep a copy of the object being constructed
          s = dup(s, node);
        }
        // for method calls (need return value)
        // for new expressions (object already duplicated)
        // and for constructor calls (also already duplicated)
        // we pop the receiver.
        s = popReceiver(mdecl, node, s, (receiverNode == null) ? node : receiverNode);
      } finally {
        lattice.setSuppressDrops(false);
      }
      if (LOG.isLoggable(Level.FINE)) {
        LOG.fine("After handling receivers/qualifiers/parameter: " + s);
      }
      if (isNormal) { // call succeeded
        if (mcall) {
          s = pushReturnValue(mdecl, s, node);
        }
        return s;
      } else {
        lattice.setAbruptResults(true);
        try {
          // we just pop all pending
          s = popAllPending(s, node);
        } finally {
          lattice.setAbruptResults(false);
        }
        return s;
      }
    }
    
    @Override
    protected Store transferCloseScope(final IRNode node, boolean flag, Store s) {
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
            s = lattice.opSet(s, node, var);
          }
        }
      }
      return s;
    }

    @Override
    protected Store transferCatchOpen(final IRNode node, Store s) {
      IRNode var = CatchClause.getParam(node);
      s = lattice.opExistingBetter(s, node, State.SHARED, mayAlias, var);
      return lattice.opSet(s, node, var);
    }
    
    @Override
    protected Store transferCatchClose(final IRNode node, boolean flag, Store s) {
      IRNode var = CatchClause.getParam(node);
      s = lattice.opNull(s);
      s = lattice.opSet(s, node, var);
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
      return push(lattice.opEqual(s, node, flag), node);
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
        s = lattice.opExisting(s, node, State.SHARED);
        if (fineIsLoggable) {
          LOG.fine("initializing static field '" + JJNode.getInfo(node) + "'");
        }
      } else {
        /* We are either a field of the class that contains the constructor
         * declaration being analyzed, or we are a field of an anonymous class
         * nested within the flow unit being analyzed.  In the first case,
         * we want the the receiver of the constructor, in the second case
         * we want the receiver of the <init> method of the anonymous class.
         */
        final IRNode rcvr;
        final IRNode enclosingType = VisitUtil.getEnclosingType(node);
        final Operator enclosingOp = JJNode.tree.getOperator(enclosingType);
        if (AnonClassExpression.prototype.includes(enclosingOp) ||
            EnumConstantClassDeclaration.prototype.includes(enclosingOp)) {
          rcvr = JavaPromise.getReceiverNode(
              JavaPromise.getInitMethod(enclosingType));
        } else {
          rcvr = JavaPromise.getReceiverNode(flowUnit);
        }
        s = lattice.opGet(s, node, rcvr);
//        s = lattice.opThis(s, node);
        if (fineIsLoggable) {
          LOG.fine("initializing field '" + JJNode.getInfo(node) + "'");
        }
      }
      s = lattice.opDup(s, node, 1); // get value on top of stack
      s = lattice.opStore(s, node, node, bcaQuery); // perform store
      s = lattice.opRelease(s, node); // throw away the extra copy
      return s;
    }
    
    @Override
    protected Store transferInitializationOfVar(final IRNode node, final Store s) {
      return lattice.opSet(s, node, node);
    }
    
    @Override
    protected Store transferLiteral(final IRNode node, final Store s) {
      final Operator op = tree.getOperator(node);
      if (NullLiteral.prototype.includes(op)) {
        return lattice.opNull(s);
      } else if (RefLiteral.prototype.includes(op)) {
        return push(s, node); // push a shared String constant
      } else {
        return lattice.push(s); // push nothing (actually the same as opNull());
      }
    }
    
    /* Original override transferIntialization(), but there is no reason to. */
    
    @Override
    protected Store transferMethodBody(final IRNode body, final Port kind, Store s) {
      if (kind instanceof EntryPort) {
        return s; // opStart() was invoked when the flow unit was entered
//        return lattice.opStart();
      } else {
        final boolean fineIsLoggable = LOG.isLoggable(Level.FINE);
        final IRNode mdecl = tree.getParent(body);
        final Operator mOp = tree.getOperator(mdecl);
        String name = "<unknown>";
        if (MethodDeclaration.prototype.includes(mOp) ||
            ConstructorDeclaration.prototype.includes(mOp)) {
          name = JJNode.getInfo(mdecl);
        }
        if (kind instanceof NormalExitPort) {
          if (MethodDeclaration.prototype.includes(mOp) &&
              !VoidType.prototype.includes(MethodDeclaration.getReturnType(mdecl))) {
            final IRNode rnode = JavaPromise.getReturnNode(mdecl);
            s = lattice.opGet(s, body, rnode);
            final UniquePromiseDrop uDrop = UniquenessRules.getUnique(rnode);
            if (uDrop != null) {
              s = lattice.opUndefine(s, body, uDrop, MessageChooser.RETURN, bcaQuery);
            } else {
              s = lattice.opCompromise(s, body);
            }
            if (fineIsLoggable) {
              LOG.fine("After handling return value of " + name + ": " + s);
            }
          }
          s = lattice.opStop(s, body);
        } else if (kind instanceof AbruptExitPort) {
          lattice.setAbruptResults(true);
          try {
            s = lattice.opStop(s, body);
          } finally {
            lattice.setAbruptResults(false);
          }
        }

        if (fineIsLoggable) {
          LOG.fine("At end of " + name + ": " + s);
        }
        return s;
      }
    }
    
    @Override
    protected Store transferRelop(IRNode node, Operator op, boolean flag, Store value) {
      if (EqualityExpression.prototype.includes(op)) {
        return super.transferRelop(node, op, flag, value);
      } else if (CompareExpression.prototype.includes(op)) {
        return lattice.push(lattice.pop(lattice.pop(value, node), node));
      } else if (InstanceOfExpression.prototype.includes(op)) {
        return lattice.push(lattice.pop(value, node));
      }
      return super.transferRelop(node, op, flag, value);
    }

    @Override
    protected Store transferReturn(IRNode node, final Store s) {
      while (node != null && !MethodDeclaration.prototype.includes(node)) {
        node = tree.getParentOrNull(node);
      }
      if (node == null) {
        return pop(s, node);
      } else {
        return lattice.opSet(s, node, JavaPromise.getReturnNode(node));
      }
    }
    
    @Override
    protected Store transferThrow(final IRNode node, final Store s) {
      return lattice.opCompromise(s, node);
    }
    
    @Override
    protected Store transferUnop(IRNode node, Operator op, Object info, Store val) {
        if (ArithUnopExpression.prototype.includes(op)) {
          return lattice.push(lattice.pop(val, node));
        } else if (ComplementExpression.prototype.includes(op)) {
          return lattice.push(lattice.pop(val, node));
        } else if (UnboxExpression.prototype.includes(op)) {
          return lattice.push(lattice.pop(val, node));
        }
      return super.transferUnop(node, op, info, val);
    }


    @Override
    protected Store transferUseArray(final IRNode aref, Store s) {
      if (!s.isValid()) return s;
      if (isBothLhsRhs(aref)) {
        s = dup(dup(s, aref), aref);
      }
      return push(pop(pop(s, aref), aref), aref);
    }
    
    @Override
    protected Store transferUseField(final IRNode fref, Store s) {
      if (!s.isValid()) return s;
      if (isBothLhsRhs(fref)) {
        s = dup(s, fref);
      }
      final IRNode decl = binder.getBinding(fref);
      if (decl == null) {
        LOG.warning("No binding for field ref " + DebugUnparser.toString(fref));
        s = lattice.pop(s, fref);
        return push(s, fref);
      } else {
        return lattice.opLoad(s, fref, decl);
      }
    }
    
    @Override
    protected Store transferUseArrayLength(final IRNode alen, Store s) {
      if (!s.isValid()) return s;
      s = lattice.pop(s, alen);
      return lattice.push(s); // push a non-pointer value (a primitive integer in this case)
    }
    
    @Override
    protected Store transferUseVar(final IRNode var, final Store s) {
      final IRNode decl = binder.getBinding(var);
      if (decl == null) {
        LOG.warning("Cannot find binding for " + DebugUnparser.toString(var));
        return push(s, var);
      } else {
        return lattice.opGet(s, var, decl);
      }
    }
    
    @Override
    protected Store transferUseReceiver(final IRNode use, final Store s) {
      return lattice.opGet(s, use, getReceiverNodeAtExpression(use));
    }
    
    @Override
    protected Store transferUseQualifiedReceiver(
        final IRNode use, final IRNode decl, final Store s) {
      // We start by getting the receiver
      Store newStore = lattice.opGet(s, use, getReceiverNodeAtExpression(use));

      /* Loop up the nested class hierarchy until we find the class whose
       * qualified receiver declaration equals 'decl'.  We are guaranteed
       * by JavaEvaluationTransfer NOT to have a qualified this expression
       * that binds to a normal receiver expression. 
       */
      IRNode currentClass = VisitUtil.getEnclosingType(use);
      IRNode currentQualifiedReceiverField;
      do {
        currentQualifiedReceiverField = JavaPromise.getQualifiedReceiverNodeOrNull(currentClass);
        // Do the pseudo-field reference
        newStore = lattice.opLoad(newStore, use, currentQualifiedReceiverField);
        currentClass = VisitUtil.getEnclosingType(currentClass);
      } while (currentQualifiedReceiverField != decl);
      
      return newStore;
    }

    /**
     * Get the receiver node appropriate for use at the given expression.
     * Normally this is the receiver node from the flow unit being analyzed,
     * unless the given node is inside a FieldDeclaration or ClassInitializer
     * that is itself inside an AnonClassExpression or EnumConstantDeclaration.
     * In that case, we use the receiver node from the InitMethod for the 
     * class expression.
     */
    private IRNode getReceiverNodeAtExpression(final IRNode use) {
      /* Need to determine if the use is inside a field init or init block
       * of an anonymous class expression.
       */
      IRNode getReceiverFrom = null;
      for (final IRNode current : VisitUtil.rootWalk(use)) {
        final Operator op = JJNode.tree.getOperator(current);
        if (ClassBody.prototype.includes(op)) {
          // done: skipped past anything potentially interesting
          getReceiverFrom = flowUnit;
          break;
        } else if (FieldDeclaration.prototype.includes(op) ||
            ClassInitializer.prototype.includes(op)) {
          /* Have to check against FieldDeclaration to avoid capturing local
           * variable initializers.  This cannot be used in a static context,
           * so don't even check for it
           */
          final IRNode enclosingType = VisitUtil.getEnclosingType(current);
          final Operator enclosingOp = JJNode.tree.getOperator(enclosingType);
          if (AnonClassExpression.prototype.includes(enclosingOp) ||
              EnumConstantClassDeclaration.prototype.includes(enclosingOp)) {
            getReceiverFrom = JavaPromise.getInitMethod(enclosingType);
            break;
          }
        }
      }
      return JavaPromise.getReceiverNode(getReceiverFrom);
    }
    
    
    
    public Store transferComponentSource(final IRNode node) {
      return lattice.opStart(mayAlias, node);
    }
  }
  
  
  
  // ==================================================================
  // === SubAnalysis factory
  // ==================================================================

  private static final class SubAnalysisFactory extends AbstractCachingSubAnalysisFactory<StoreLattice, Store> {
    private final AtomicBoolean cargo;
    private final IRNode flowUnit;
    private final boolean timeOut;
    private final IMayAlias mayAlias;
    private final BindingContextAnalysis.Query bcaQuery;
    

    
    public SubAnalysisFactory(final AtomicBoolean c,
        final IRNode fu, final IMayAlias ma, final boolean to,
        BindingContextAnalysis.Query query) {
      cargo = c;
      flowUnit = fu;
      timeOut = to;
      mayAlias = ma;
      bcaQuery = query;
    }
    
    @Override
    protected JavaForwardAnalysis<Store, StoreLattice> realCreateAnalysis(
        final IRNode caller, final IBinder binder, final StoreLattice lattice,
        final Store initialValue, final boolean terminationNormal) {
      final int floor = initialValue.isValid() ? initialValue.getStackSize().intValue() : 0;
      final UniquenessTransfer transfer = 
        new UniquenessTransfer(cargo, binder, mayAlias, lattice,
            bcaQuery.getSubAnalysisQuery(caller), floor, flowUnit, timeOut);
      return new Uniqueness(false, cargo, "Sub Analysis", lattice, transfer, timeOut);
    }
  }
  
  
  
  // ==================================================================
  // === Queries
  // ==================================================================

  public static final String NOT_AN_ERROR = "Usage is correct.";

  private static final boolean isBadState(final StoreLattice sl, final Store s) {
    return s != null && !sl.equals(s, sl.bottom()) && !s.isValid();
  }
  
  public IsInvalidQuery getIsInvalidQuery(final IRNode flowUnit) {
    return new IsInvalidQuery(getAnalysisThunk(flowUnit));
  }
  
  public IsPositivelyAssuredQuery getIsPositivelyAssuredQuery(final IRNode flowUnit) {
    return new IsPositivelyAssuredQuery(getAnalysisThunk(flowUnit));
  }
  
  public NormalErrorQuery getNormalErrorQuery(final IRNode flowUnit) {
    return new NormalErrorQuery(getAnalysisThunk(flowUnit));
  }
  
  public AbruptErrorQuery getAbruptErrorQuery(final IRNode flowUnit) {
    return new AbruptErrorQuery(getAnalysisThunk(flowUnit));
  }
  
  public RawQuery getRaw(final IRNode flowUnit) {
    return new RawQuery(getAnalysisThunk(flowUnit));
  }
  
  
  
  public static class IsInvalidQuery extends AbstractJavaFlowAnalysisQuery<IsInvalidQuery, Boolean, Store, StoreLattice> {
    protected IsInvalidQuery(
        final IThunk<? extends IJavaFlowAnalysis<Store, StoreLattice>> thunk) {
      super(thunk);
    }

    protected IsInvalidQuery(
        final Delegate<IsInvalidQuery, Boolean, Store, StoreLattice> d) {
      super(d);
    }

    @Override
    protected Boolean getBottomReturningResult(
        final StoreLattice lattice, final IRNode expr) {
      /* This method should return a result equivalent to
       * getEvaluatedAnalysisResult() where all the returned analysis values
       * are BOTTOM.  In this case, this simplifies to FALSE because
       * if you assume 'sbefore' is BOTTOM, then 
       * "sbefore != null && !lattice.equals(sbefore, top) && !sbefore.isValid()"
       * is true because BOTTOM.isValid() is false. 
       */
      return Boolean.FALSE;
    }

    @Override
    protected Boolean getEvaluatedAnalysisResult(
        final IJavaFlowAnalysis<Store, StoreLattice> analysis,
        final StoreLattice lattice, final IRNode expr) {
      final Store sbefore = analysis.getAfter(expr, WhichPort.ENTRY);
      final Store safter = analysis.getAfter(expr, WhichPort.NORMAL_EXIT);
      final Store sabrupt = analysis.getAfter(expr, WhichPort.ABRUPT_EXIT);

      // A node is invalid if things were OK when the store
      // came in, but are wrong now that control is leaving.
      // ? NB: bottom() sometimes means control didn't get to
      // ? a place but also happens near the start of a procedure
      // ? before OpStart() and so we can't ignore bottom(). Also,
      // ? I believe bottom().isValid is false. JTB 2002/9/23

      // If the state coming in is bad, no error:
      if (isBadState(lattice, sbefore)) {
        return Boolean.FALSE;
      }

      // If the state coming out for normal termination is bad, an error:
      if (isBadState(lattice, safter)) {
        return Boolean.TRUE;
      }

      // If the state coming out for abrupt termination is bad, an error:
      if (isBadState(lattice, sabrupt)) {
        return Boolean.TRUE;
      }

      // Otherwise, must be OK
      return Boolean.FALSE;
    }

    @Override
    protected IsInvalidQuery newSubAnalysisQuery(
        final Delegate<IsInvalidQuery, Boolean, Store, StoreLattice> delegate) {
      return new IsInvalidQuery(delegate);
    }
  }
  
  
  
  public static class IsPositivelyAssuredQuery extends AbstractJavaFlowAnalysisQuery<IsPositivelyAssuredQuery, Boolean, Store, StoreLattice> {
    protected IsPositivelyAssuredQuery(
        final IThunk<? extends IJavaFlowAnalysis<Store, StoreLattice>> thunk) {
      super(thunk);
    }

    protected IsPositivelyAssuredQuery(
        final Delegate<IsPositivelyAssuredQuery, Boolean, Store, StoreLattice> d) {
      super(d);
    }

    @Override
    protected Boolean getBottomReturningResult(
        final StoreLattice lattice, final IRNode expr) {
      /* getEvaluatedAnaysisResults() with BOTTOM substituted in for safter and 
       * sabrupt.
       */
      return Boolean.FALSE;
    }

    @Override
    protected Boolean getEvaluatedAnalysisResult(
        final IJavaFlowAnalysis<Store, StoreLattice> analysis,
        final StoreLattice lattice, final IRNode expr) {
      final Store safter = analysis.getAfter(expr, WhichPort.NORMAL_EXIT);
      final Store sabrupt = analysis.getAfter(expr, WhichPort.ABRUPT_EXIT);
      return !isBadState(lattice, safter) && !isBadState(lattice,sabrupt);
    }

    @Override
    protected IsPositivelyAssuredQuery newSubAnalysisQuery(
        edu.cmu.cs.fluid.java.analysis.AbstractJavaFlowAnalysisQuery.Delegate<IsPositivelyAssuredQuery, Boolean, Store, StoreLattice> delegate) {
      return new IsPositivelyAssuredQuery(delegate);
    }
  }
  
  
  
  public static final class NormalErrorQuery extends SimplifiedJavaFlowAnalysisQuery<NormalErrorQuery, String, Store, StoreLattice> {
    protected NormalErrorQuery(
        final IThunk<? extends IJavaFlowAnalysis<Store, StoreLattice>> thunk) {
      super(thunk);
    }

    protected NormalErrorQuery(
        final Delegate<NormalErrorQuery, String, Store, StoreLattice> d) {
      super(d);
    }

    @Override
    protected RawResultFactory getRawResultFactory() {
      return RawResultFactory.NORMAL_EXIT;
    }

    @Override
    protected String processRawResult(
        final IRNode expr, final StoreLattice lattice, final Store rawResult) {
      if (isBadState(lattice, rawResult)) {
        return lattice.toString(rawResult);
      } else {
        return NOT_AN_ERROR;
      }      
    }

    @Override
    protected NormalErrorQuery newSubAnalysisQuery(
        final Delegate<NormalErrorQuery, String, Store, StoreLattice> delegate) {
      return new NormalErrorQuery(delegate);
    }
  }
  
  
  
  public static final class AbruptErrorQuery extends SimplifiedJavaFlowAnalysisQuery<AbruptErrorQuery, String, Store, StoreLattice> {
    protected AbruptErrorQuery(
        final IThunk<? extends IJavaFlowAnalysis<Store, StoreLattice>> thunk) {
      super(thunk);
    }

    protected AbruptErrorQuery(
        final Delegate<AbruptErrorQuery, String, Store, StoreLattice> d) {
      super(d);
    }

    @Override
    protected RawResultFactory getRawResultFactory() {
      return RawResultFactory.ABRUPT_EXIT;
    }

    @Override
    protected String processRawResult(
        final IRNode expr, final StoreLattice lattice, final Store rawResult) {
      if (isBadState(lattice, rawResult)) {
        return lattice.toString(rawResult);
      } else {
        return NOT_AN_ERROR;
      }      
    }

    @Override
    protected AbruptErrorQuery newSubAnalysisQuery(
        final Delegate<AbruptErrorQuery, String, Store, StoreLattice> delegate) {
      return new AbruptErrorQuery(delegate);
    }
  }
  
  public static final class RawQuery extends SimplifiedJavaFlowAnalysisQuery<RawQuery, Store, Store, StoreLattice> {
    protected RawQuery(
        final IThunk<? extends IJavaFlowAnalysis<Store, StoreLattice>> thunk) {
      super(thunk);
    }

    protected RawQuery(
        final Delegate<RawQuery, Store, Store, StoreLattice> d) {
      super(d);
    }

    @Override
    protected RawResultFactory getRawResultFactory() {
      return RawResultFactory.ENTRY;
    }

    @Override
    protected Store processRawResult(
        final IRNode expr, final StoreLattice lattice, final Store rawResult) {
      return rawResult;
    }

    @Override
    protected RawQuery newSubAnalysisQuery(
        final Delegate<RawQuery, Store, Store, StoreLattice> delegate) {
      return new RawQuery(delegate);
    }
  }
  
  private static final class ReceiverSnatcher extends JavaSemanticsVisitor {
    private final List<IRNode> refs;
    
    private ReceiverSnatcher(final IRNode flowUnit, final List<IRNode> refs) {
      super(false, flowUnit);
      this.refs = refs;
    }
    
    public static void getReceivers(final IRNode flowUnit, final List<IRNode> refs) {
      final ReceiverSnatcher rs = new ReceiverSnatcher(flowUnit, refs);
      rs.doAccept(flowUnit);
    }
    
    @Override
    protected void handleAnonClassExpression(final IRNode anonClass) {
      super.handleAnonClassExpression(anonClass);
      // Add the receiver from <init>
      refs.add(JavaPromise.getReceiverNode(JavaPromise.getInitMethod(anonClass)));
    }
  }
}
