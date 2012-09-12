package com.surelogic.analysis.uniqueness.plusFrom.sideeffecting;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.analysis.AbstractWholeIRAnalysis;
import com.surelogic.analysis.IBinderClient;
import com.surelogic.analysis.JavaSemanticsVisitor;
import com.surelogic.analysis.LocalVariableDeclarations;
import com.surelogic.analysis.alias.IMayAlias;
import com.surelogic.analysis.alias.TypeBasedMayAlias;
import com.surelogic.analysis.effects.Effect;
import com.surelogic.analysis.effects.Effects;
import com.surelogic.analysis.effects.targets.InstanceTarget;
import com.surelogic.analysis.effects.targets.Target;
import com.surelogic.analysis.regions.IRegion;
import com.surelogic.analysis.uniqueness.plusFrom.sideeffecting.state.BuriedMessage;
import com.surelogic.analysis.uniqueness.plusFrom.sideeffecting.state.RealSideEffects;
import com.surelogic.analysis.uniqueness.plusFrom.sideeffecting.store.State;
import com.surelogic.analysis.uniqueness.plusFrom.sideeffecting.store.Store;
import com.surelogic.analysis.uniqueness.plusFrom.sideeffecting.store.StoreLattice;
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
import edu.cmu.cs.fluid.ide.IDE;
import edu.cmu.cs.fluid.ide.IDEPreferences;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaNames;
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
import edu.cmu.cs.fluid.java.operator.BoxExpression;
import edu.cmu.cs.fluid.java.operator.CallInterface;
import edu.cmu.cs.fluid.java.operator.CatchClause;
import edu.cmu.cs.fluid.java.operator.ClassBody;
import edu.cmu.cs.fluid.java.operator.ClassInitializer;
import edu.cmu.cs.fluid.java.operator.CompareExpression;
import edu.cmu.cs.fluid.java.operator.ComplementExpression;
import edu.cmu.cs.fluid.java.operator.ConstructorCall;
import edu.cmu.cs.fluid.java.operator.ConstructorDeclaration;
import edu.cmu.cs.fluid.java.operator.DeclStatement;
import edu.cmu.cs.fluid.java.operator.DimExprs;
import edu.cmu.cs.fluid.java.operator.EnumConstantClassDeclaration;
import edu.cmu.cs.fluid.java.operator.EnumConstantDeclaration;
import edu.cmu.cs.fluid.java.operator.EqualityExpression;
import edu.cmu.cs.fluid.java.operator.FieldDeclaration;
import edu.cmu.cs.fluid.java.operator.InstanceOfExpression;
import edu.cmu.cs.fluid.java.operator.MethodCall;
import edu.cmu.cs.fluid.java.operator.MethodDeclaration;
import edu.cmu.cs.fluid.java.operator.NestedClassDeclaration;
import edu.cmu.cs.fluid.java.operator.NormalEnumConstantDeclaration;
import edu.cmu.cs.fluid.java.operator.NullLiteral;
import edu.cmu.cs.fluid.java.operator.OuterObjectSpecifier;
import edu.cmu.cs.fluid.java.operator.ParameterDeclaration;
import edu.cmu.cs.fluid.java.operator.QualifiedThisExpression;
import edu.cmu.cs.fluid.java.operator.RefLiteral;
import edu.cmu.cs.fluid.java.operator.SimpleEnumConstantDeclaration;
import edu.cmu.cs.fluid.java.operator.SomeFunctionDeclaration;
import edu.cmu.cs.fluid.java.operator.StringConcat;
import edu.cmu.cs.fluid.java.operator.StringLiteral;
import edu.cmu.cs.fluid.java.operator.SuperExpression;
import edu.cmu.cs.fluid.java.operator.TypeDeclarationStatement;
import edu.cmu.cs.fluid.java.operator.UnboxExpression;
import edu.cmu.cs.fluid.java.operator.VarArgsExpression;
import edu.cmu.cs.fluid.java.operator.VariableDeclaration;
import edu.cmu.cs.fluid.java.operator.VariableDeclarators;
import edu.cmu.cs.fluid.java.operator.VariableUseExpression;
import edu.cmu.cs.fluid.java.operator.VoidType;
import edu.cmu.cs.fluid.java.promise.QualifiedReceiverDeclaration;
import edu.cmu.cs.fluid.java.promise.ReceiverDeclaration;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.WarningDrop;
import edu.cmu.cs.fluid.sea.drops.effects.RegionEffectsPromiseDrop;
import edu.cmu.cs.fluid.sea.drops.promises.UniquenessControlFlowDrop;
import edu.cmu.cs.fluid.sea.proxy.ResultDropBuilder;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.Iteratable;
import edu.uwm.cs.fluid.control.FlowAnalysis;
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
  
  // for creating drops
  private final AbstractWholeIRAnalysis<UniquenessAnalysis,?> analysis;

  
  
  // ==================================================================
  // === Constructor 
  // ==================================================================
  
  public UniquenessAnalysis(
      final AbstractWholeIRAnalysis<UniquenessAnalysis,?> a,
      final IBinder binder, final boolean to) {
    super(new FixBinder(binder)); // avoid crashes.
    mayAlias = new TypeBasedMayAlias(binder);
    analysis = a;
    timeOut = to;
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
     *  receiver from the <init> method  (BUG 1712)
     */
    ReceiverSnatcher.getReceivers(flowUnit, refLocals);
    
    final IRNode[] locals = refLocals.toArray(new IRNode[refLocals.size()]);
    final Effects effects = new Effects(binder);
    final List<Effect> methodEffects = effects.getMethodEffects(flowUnit, flowUnit);
    final StoreLattice lattice =
        new StoreLattice(locals, binder, mayAlias, methodEffects);
    final AtomicBoolean reworkFlag = new AtomicBoolean(false);
    return new UniquenessRoot(flowUnit, analysis, reworkFlag,
        "Uniqueness Analsys (U+F)", lattice,
        new UniquenessTransfer(
            reworkFlag, binder, effects, lattice, 0, flowUnit, timeOut),
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
  // === Flow Analysis
  // ==================================================================
 
  private static final class UniquenessRoot extends JavaForwardAnalysis<Store, StoreLattice> {
    private static final long NANO_SECONDS_PER_SECOND = 1000000000L;

    private final AtomicBoolean reworkFlag;
    private final IRNode flowUnit;
    private final AbstractWholeIRAnalysis<UniquenessAnalysis,?> analysis;

    public UniquenessRoot(final IRNode fu,
        AbstractWholeIRAnalysis<UniquenessAnalysis,?> a,
        final AtomicBoolean f,
        final String name, final StoreLattice lattice,
        final UniquenessTransfer transfer, boolean timeOut) {
      super(name, lattice, transfer, DebugUnparser.viewer, timeOut);
      reworkFlag = f;
      flowUnit = fu;
      analysis = a;
    }
    
    @Override
    public void performAnalysis() {
      final String methodName =
          JavaNames.genQualifiedMethodConstructorName(flowUnit);
      final UniquenessControlFlowDrop controlFlowDrop =
          new UniquenessControlFlowDrop(flowUnit);
      
      final long tooLongDuration = IDE.getInstance().getIntPreference(
          IDEPreferences.TIMEOUT_WARNING_SEC) * NANO_SECONDS_PER_SECOND;
      final long startTime = System.nanoTime();
      boolean gaveUp = false;
      
      /* Do the analysis without side-effects.  Trap for time-out errors. */
      try {
        realPerformAnalysis();
        
        // Did we take too long?
        final long endTime = System.nanoTime();
        final long duration = endTime - startTime;
        if (duration > tooLongDuration) {
          final WarningDrop info = new WarningDrop();
          analysis.setResultDependUponDrop(info, flowUnit);
          info.setResultMessage(Messages.TOO_LONG, tooLongDuration / NANO_SECONDS_PER_SECOND,
              methodName, duration / NANO_SECONDS_PER_SECOND);
          info.setCategory(Messages.DSC_UNIQUENESS_LONG_RUNNING);
          controlFlowDrop.addDependent(info);
        }
      } catch (final FlowAnalysis.AnalysisGaveUp e) {
        // Analysis of the flow unit gave up
        final long endTime = System.nanoTime();
        final long duration = endTime - startTime;
        gaveUp = true;
        
        final ResultDropBuilder timeOutResult = ResultDropBuilder.create(analysis);
        analysis.setResultDependUponDrop(timeOutResult, flowUnit);
        timeOutResult.setTimeout();
        timeOutResult.setCategory(Messages.DSC_UNIQUENESS_TIMEOUT);
        timeOutResult.setResultMessage(Messages.TIMEOUT,
            e.timeOut / NANO_SECONDS_PER_SECOND,
            methodName, duration / NANO_SECONDS_PER_SECOND);
        timeOutResult.addCheckedPromise(controlFlowDrop);
          
        /* TODO: Add "time out" result to
         * (2) Borrowed promises of the method's
         * parameters, and (3) Unique promise on the method's return node,
         */
      }

      if (!gaveUp) {
        // Turn on side effects and rerun analysis one more time
        final RealSideEffects se = new RealSideEffects(controlFlowDrop, analysis);
        lattice.setSideEffects(se);
        reworkFlag.set(true);
        reworkAll();
        
        // Dump the results into the sea
        se.makeResultDrops();
      }
    }
  }
  
  private static final class UniquenessChild extends JavaForwardAnalysis<Store, StoreLattice> {
    private final AtomicBoolean reworkFlag;

    public UniquenessChild(
        final AtomicBoolean f, final String name, final StoreLattice lattice,
        final UniquenessTransfer transfer, boolean timeOut) {
      super(name, lattice, transfer, DebugUnparser.viewer, timeOut);
      reworkFlag = f;
    }
    
    @Override
    public void performAnalysis() {
      if (reworkFlag.get()) {
        reworkAll();
      } else {
        realPerformAnalysis();
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
    private final Effects effects;
    
    
    
    // ==================================================================
    // === Constructor 
    // ==================================================================

    public UniquenessTransfer(
        final AtomicBoolean reworkFlag, final IBinder binder, final Effects fx,
        final StoreLattice lattice, final int floor, final IRNode fu,
        final boolean timeOut) {
      super(binder, lattice, new SubAnalysisFactory(reworkFlag, fu, timeOut), floor);
      flowUnit = fu;
      effects = fx;
    }

    
    
    // ==================================================================
    // === Methods from JavaEvaluationTransfer 
    // ==================================================================

    /** Pop and discard value from top of stack */
    @Override
    protected Store pop(final Store s, final IRNode srcOp) {
      return lattice.opRelease(s, srcOp);
    }

    /** Push an unknown shared value onto stack. */
    @Override
    protected Store push(final Store s, final IRNode srcOp) {
    	throw new FluidError("push called!");
      // return lattice.opExisting(s, State.SHARED);
    }
    
    /** Remove the second from top value from stack */
    @Override
    protected Store popSecond(final Store s, final IRNode srcOp) {
      if (!s.isValid()) return s;
      return lattice.opSet(s, srcOp, StoreLattice.getUnderTop(s));
    }

    @Override
    protected Store dup(final Store s, final IRNode srcOp) {
      if (!s.isValid()) return s;
      return lattice.opGet(s, srcOp, StoreLattice.getStackTop(s));
    }
    
    /** Remove all pending values from stack */
    @Override
    protected Store popAllPending(Store s, final IRNode srcOp) {
      while (s.isValid() && StoreLattice.getStackTop(s).intValue() > 0) {
        s = pop(s, srcOp);
      }
      return s;
    }

    
    
    /**
     * True if we wish to check effects disjointness.
     * This should be off for now, especially since we don't use
     * effects disjointness to initialize the store.
     */
    private static boolean CHECK_EFFECTS_DISJOINTNESS = false;
    
    
    // ==================================================================
    // === Other Helper Methods 
    // ==================================================================

    private Store considerDeclaredEffects(
        final IRNode callSrcOp, final IRNode mdecl, final boolean hasOuter,
        final IRNode actualRcvr,
        final int numFormals, final IRNode formals,
        final RegionEffectsPromiseDrop fxDrop,
        final List<Effect> declEffects, Store s) {
      for (final Effect f : declEffects) {
    	if (!s.isValid()) return s;
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
        final Integer actualStackDepth = getActualDepth(ref, numFormals, formals);
        
        if (f.isWrite() && CHECK_EFFECTS_DISJOINTNESS) {
            // A write effect on a reference cannot 
            // overlap with any effect on the same reference.        
        	for (final Effect f2 : declEffects) {
        		if (f2.isEmpty()) continue;
        		if (f == f2) continue; // Don't compare with self.
        		Target t2 = f2.getTarget();
        		if (t.getRegion().overlapsWith(t2.getRegion())) {
        			final Integer asd2 = getActualDepth(ref, numFormals, formals);
        			// if (asd2 != actualStackDepth) continue;
        			int total = s.getStackSize();
        			if (actualStackDepth == null || asd2 == null || 
        					lattice.mayAlias(s,total-actualStackDepth, total-asd2)) {
        				String message = "Effect disjointness error: " + f + " overlaps with " + f2;
        				System.out.println(message);
        				return lattice.errorStore(message);
        			}
        		}
        	}
        }
        
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
        if (actualStackDepth == null) {
        	s = lattice.opExisting(s, callSrcOp, State.SHARED, null);
        } else {
        	s = lattice.opDup(s, callSrcOp, actualStackDepth); 
        }
        
        if (!s.isValid()) return s; // somehow opExisting (?) is finding an error
      
        final IRegion r = t.getRegion();
        if (r.isAbstract()) {
            // it could refer to almost anything
            // everything reachable from this pointer is alias buried:
        	s = lattice.opLoadReachable(s, callSrcOp, fxDrop);
        	s = lattice.opRelease(s, callSrcOp);
        } else {
        	// it's a field, load and discard.
        	s = lattice.opLoad(s, callSrcOp, r.getNode());
        	s = lattice.opRelease(s, callSrcOp);
        }
        
        if (f.isWrite()) {
          /* Loop over each formal (including the receiver) (qualified receivers
           * cannot be @Immutable or @ReadOnly) and see if the effect covers
           * an effect on the state of the object referenced by the formal
           * parameter.
           */
          if (!TypeUtil.isStatic(mdecl)) {
            final IRNode rcvr = JavaPromise.getReceiverNode(mdecl);
            final int depth = hasOuter ? numFormals + 1 : numFormals;
            s = checkMutationOfParameters(s, t, rcvr, depth); // XXX: May be generating bogus errors from opGet()
          }
          
          for (int i = 0; i < numFormals; ++i) {
            // Create "writes(p:Instance)"
            final IRNode formal = tree.getChild(formals, i);
            s = checkMutationOfParameters(
                s, t, formal, computeDepthOfFormal(numFormals, i)); // XXX: May be generating bogus errors from opGet()
          }
        }
      }
      return s;
    }

    private Store checkMutationOfParameters(
        Store s, final Target declaredTarget,
        final IRNode formal, final int stackDepth) {
      if (declaredTarget.mayTargetStateOfReference(binder, formal)) {
        // State of the object passed as a parameter may be affected
        s = lattice.opDup(s, formal, stackDepth);
        if (!s.isValid()) return s;
        // check for sneaky mutations
        s = lattice.opCheckMutable(s, StoreLattice.getStackTop(s));
        s = pop(s, formal);
      }
      return s;
    }
    
	/**
	 * Get the stack depth of the actual parameter connected to this
	 * formal parameter (or receiver or qualified receiver).
	 * (0 means on the top).
	 * @param formal formal parameter IRNode
	 * @param numFormals number of formals
	 * @param formals sequence of formals (or null if numFormals is 0)
	 * @return stack depth of actual parameter
	 */
    private Integer getActualDepth(final IRNode formal, final int numFormals,
    		final IRNode formals) {
    	if (formal == null) return null;
    	
    	if (QualifiedReceiverDeclaration.prototype.includes(formal)) return numFormals+1;
    	if (ReceiverDeclaration.prototype.includes(formal)) return numFormals;
    	
    	for (int i = 0; i < numFormals; ++i) {
    		if (tree.getChild(formals, i).equals(formal)) {
    			return computeDepthOfFormal(numFormals, i);
    		}
    	}

    	return null;
    }

  private static int computeDepthOfFormal(final int numFormals, final int i) {
    return numFormals - i - 1;  // was + 1; fixed 2011-01-07
  }

//    /**
//     * Return true if the given region may have a field in the given class that is
//     * unique.
//     */
//    private boolean regionHasUniqueFieldInClass(IRegion reg, IRNode cd) {
//      // One possibility is to enumerate the fields of the class
//      // and ask each one if they are in the region.
//      //
//      // ! For now, assume the worst:
//      return true;
//    }

    private final String RETURN_VAR = "result".intern();
    
    /**
     * Add a from triple from top of the stack to the result of this call.
     * @param s store before
     * @return start after
     */
    private Store addFromNode(Store s) {
    	if (!s.isValid()) return s;
    	return lattice.opConnect(s, StoreLattice.getStackTop(s), StoreLattice.fromField, RETURN_VAR);
    }

    /**
     * Return the store after popping off and processing each actual parameter
     * according to the formal parameters. The number should be the same (or else
     * how did the binder determine to call this method/constructor?).
     * The formals may be null only if there is some sort of error.
     * This error is logged as a warning already.
     */
    // Only called with numActuals > 0
    private Store popArguments(
        final int numActuals, final IRNode actuals, final IRNode formals, Store s) {
      if (formals != null && numActuals != tree.numChildren(formals)) {
        throw new FluidError("#formals != #actuals");
      }
      
      /* Handle varargs: If the last actual argument is a VarArgsExpression, we 
       * have to deal with the fact that in reality the final argument is a 
       * an array: That is, if the called method is 
       * 
       *   void foo(int x, Object... y) { ... }
       *   
       * and the call is "foo(a, b, c, d)", we really have the call
       * "foo(a, new Object[] { b, c, d, })".  So we need to simulate the 
       * array assignments for the last 3 arguments, and then push a new 
       * object on the stack to account for the array.  Then we check that 
       * new object against the last declared formal parameter.
       */
      final IRNode lastActual = JJNode.tree.getChild(actuals, numActuals - 1);
      if (VarArgsExpression.prototype.includes(lastActual)) {
        // compromise each actual argument that is part of the var args expression
        final int numActualsInArray = JJNode.tree.numChildren(lastActual);
        for (int count = numActualsInArray - 1; count >= 0; count--) {
          if (!s.isValid()) return s;
          s = lattice.opCompromise(s, JJNode.tree.getChild(lastActual, count),
              Messages.COMPROMISED_BY_VARARGS_ASSIGNMENT,
              DebugUnparser.toString(lastActual));
        }
        if (!s.isValid()) return s;
        // push a new object to represent the array
        s = lattice.opNew(s);
      } else {
        /* Make sure we have a valid store below; we have a valid store coming
         * out of the true branch already.
         */
        if (!s.isValid()) return s;
      }
      
      for (int n = numActuals - 1; n >= 0; n--) {
        final IRNode formal = formals != null ? tree.getChild(formals, n) : null;
        if (formal != null && UniquenessRules.isBorrowed(formal) && 
        		UniquenessRules.getBorrowed(formal).allowReturn()) {
        	s = addFromNode(s);
        	System.out.println("Popping a borrowed(allowReturn) actual, state is " + lattice.toString(s));
        }
        if (lattice.isValueNode(formal)) {
          s = pop(s, formal);
        } else {
          final IRNode srcOp = JJNode.tree.getChild(actuals, n);
          final State required = lattice.declStatus(formal);
          if (required == State.SHARED) {
            s = lattice.opConsumeShared(s, srcOp,
                Messages.COMPROMISED_BY_SHARED_FORMAL,
                ParameterDeclaration.getId(formal));
          } else if (required == State.UNIQUE) {
            s = lattice.opConsumeUnique(s, srcOp);
          } else {
            s = lattice.opConsume(s, srcOp, required);
          }
        }
      }
      return s;
    }

    /**
     * The top of the stack was a receiver of a method call, pop it off using one
     * of the three methods for popping depending on the promises/demands about
     * the receiver.
     */
    private Store popReceiver(final IRNode decl, Store s, final IRNode srcOp) {
    	if (decl == null || JavaNode.getModifier(decl, JavaNode.STATIC)) {
    	  return pop(s, srcOp);
    	}
    	
      final IRNode recDecl = JavaPromise.getReceiverNode(decl);

      if (UniquenessRules.isBorrowed(recDecl) && 
      		UniquenessRules.getBorrowed(recDecl).allowReturn()) {
      	s = addFromNode(s);
      	System.out.println("Popping a borrowed(allowReturn) actual, state is " + lattice.toString(s));
      }
      
      if (lattice.isValueNode(recDecl)) {
        return pop(s, srcOp); // do nothing.  Type system will check
      } else {
        final State required = lattice.receiverStatus(decl, recDecl);
        if (required == State.SHARED) {
          return lattice.opConsumeShared(
              s, srcOp, Messages.COMPROMISED_BY_SHARED_RECEIVER);
        } else if (required == State.UNIQUE) {
          return lattice.opConsumeUnique(s, srcOp);
        } else {
          return lattice.opConsume(s, srcOp, required);
        }
      }
    }

    /**
     * The top of the stack was a receiver of a inner class constructor call, pop it off using one
     * of the three methods for popping depending on the promises/demands about
     * the qualified receiver.
     */
    private Store popQualifiedReceiver(
        final IRNode decl, Store s, final IRNode srcOp) {
    	if (decl == null) return pop(s, srcOp);
    	
    	IRNode p = JJNode.tree.getParent(decl);
    	while (p != null && !(JJNode.tree.getOperator(p) instanceof NestedClassDeclaration))
    		p = JJNode.tree.getParent(p);
    	
    	if (TypeDeclarationStatement.prototype.includes(JJNode.tree.getParent(p))) {
    		// a NCD inside the current method
    		return transferNestedClassUse(p,pop(s, srcOp));
    	}
    	
    	final IRNode qr = JavaPromise.getQualifiedReceiverNodeOrNull(decl);
    	if (UniquenessRules.isBorrowed(qr) && UniquenessRules.getBorrowed(qr).allowReturn()) {
    		s = addFromNode(s);
    		System.out.println("Found Borrowed(allowReturn) qualified receiver, now " + lattice.toString(s));
    	}

    	if (lattice.isValueNode(qr)) { // unlikely
    		return pop(s, srcOp);
    	} else {
    	  final State required = lattice.declStatus(qr);
    	  if (required == State.SHARED) {
    	    return lattice.opConsumeShared(
    	        s, srcOp, Messages.COMPROMISED_BY_SHARED_QUALIFIED_RECEIVER);
    	  } else if (required == State.UNIQUE) {
    	    return lattice.opConsumeUnique(s, srcOp);
    	  } else {
    	    return lattice.opConsume(s, srcOp, required);
    	  }
    	}
    }

    /**
     * Push an abstract value corresponding to the return decl of the method or
     * constructor declared in decl.
     */
    private Store pushReturnValue(final IRNode decl, final Store s, final IRNode srcOp) {
      if (decl == null) {
        return lattice.opNull(s);
      }
      // Will crash if given a ConstructorDeclaration
      final Operator op = JJNode.tree.getOperator(decl);
      final IRNode ty;
      if (MethodDeclaration.prototype.includes(op)) {
        ty = MethodDeclaration.getReturnType(decl);
      } else {
    	ty = AnnotationElement.getType(decl);
      }
      if (VoidType.prototype.includes(ty)) {
        return lattice.opNull(s);
      }
      
      final IRNode retDecl = JavaPromise.getReturnNodeOrNull(decl);
      if (retDecl == null) {
        LOG.severe("Method missing return value declaration");
        return lattice.opNull(s);
      }
      
      return lattice.opGenerate(s, srcOp, lattice.declStatus(retDecl), retDecl);
    }

    
    
    // ==================================================================
    // === Transfer function methods 
    // ==================================================================

    @Override
    protected Store transferAllocation(final IRNode node, final Store s) {
      return lattice.opNew(s);
    }
    
    @Override
    protected Store transferAnonClass(final IRNode node, Store s) {
    	return transferNestedClassUse(node,s);
    }
    
    @Override
    protected Store transferArrayCreation(final IRNode node, Store val) {
    	// pop dimensions
    	if (tree.getOperator(node) instanceof DimExprs) {
    		val = pop(val, node, tree.numChildren(node));
    	}
    	// push new array
    	return lattice.opNew(val);
    }
    
    @Override
    protected Store transferArrayInitializer(final IRNode node, final Store s) {
    	// System.out.println("At array initializer, store is " + lattice.toString(s));
      return lattice.opCompromise(s, JJNode.tree.getParent(node),
          Messages.COMPROMISED_BY_ARRAY_INITIALIZER,
          DebugUnparser.toString(JJNode.tree.getParent(node)));
    }
    
    @Override
    protected Store transferAssignArray(final IRNode node, Store s) {
      // [..., arrayRef, idx, val]: pop idx
      s = popSecond(s, node);
      if (!s.isValid()) return s;
      // [..., arrayRef, val]: Check for mutability of the arrayRef
      s = lattice.opCheckMutable(s, StoreLattice.getUnderTop(s));
      if (!s.isValid()) return s;
      // [..., arrayRef, val]: Pop arrayRef, and compromise val
      return lattice.opCompromiseNoRelease(
          popSecond(s, node), node, Messages.COMPROMISED_BY_ARRAY_ASSIGNMENT,
          DebugUnparser.toString(node));
    }
    
    @Override
    protected Store transferAssignField(final IRNode node, Store s) {
      final IRNode fieldDecl = binder.getBinding(node);
      if (fieldDecl == null) {
        LOG.warning("field not bound" + DebugUnparser.toString(node));
        return popSecond(s, node);
      } else {
        if (!s.isValid()) return s;
        final Integer object = StoreLattice.getUnderTop(s);
        final Integer field = StoreLattice.getStackTop(s);
        // first copy both onto stack
        s = lattice.opGet(lattice.opGet(s, node, object), node, field);
        // now perform assignment
        s = lattice.opStore(s, node, fieldDecl);
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
    	if (op instanceof StringConcat) {
    		return lattice.opValue(
    		    lattice.opRelease(
    		        lattice.opRelease(val, node), node), node);
    	} else {
    		return lattice.opNull(pop(pop(val, node), node));
    	}
    }

    @Override
    protected Store transferCall(final IRNode node, final boolean flag, Store s) {
      final IRNode mdecl = binder.getBinding(node);
      final Operator op = tree.getOperator(node);
      IRNode actuals;
      try {
        actuals = ((CallInterface) op).get_Args(node);
      } catch (final CallInterface.NoArgs e) {
        actuals = null;
      }
      
      final IRNode outerObjectSpec = getOuterObject(node);
      final IRNode srcOp = outerObjectSpec == null ? node : outerObjectSpec;
      final boolean isMethodCall = MethodCall.prototype.includes(op);
      final IRNode receiverNode = 
          isMethodCall ? ((MethodCall) op).get_Object(node) : null;
      return handleCallLikeExpressions(
          node, flag, s, srcOp, mdecl, actuals, outerObjectSpec,
          isMethodCall, receiverNode);
    }

    /**
     * Process a method/constructor call.  Shared by {@link #transferCall}
     * and {@link #transferImpliedNewExpression}.
     *  
     * @param node The node of the method/constructor call.
     * @param flag <code>true</code> for normal method termination
     * @param s The store.
     * @param srcOp The call site for linking errors.
     * @param mdecl The declaration node of the called method/constructor.
     * @param actuals 
     * @param formals
     * @param hasOuter
     * @param isMethodCall
     * @return
     */
    private Store handleCallLikeExpressions(final IRNode node,
        final boolean isNormal, Store s, final IRNode srcOp,
        final IRNode mdecl, final IRNode actuals,
        final IRNode outerSpec, final boolean isMethodCall,
        final IRNode receiverNode) {
      final IRNode formals = SomeFunctionDeclaration.getParams(mdecl);

      // Should be the same for actuals and formals
      final int numArgs = JJNode.tree.numChildren(formals);

      if (!isNormal) {
        lattice.setSuppressDrops(true);
      }
      try {
        s = considerDeclaredEffects(
            srcOp, mdecl, outerSpec != null, receiverNode, numArgs, formals,
            MethodEffectsRules.getRegionEffectsDrop(mdecl),
            effects.getMethodEffects(mdecl, node), s);
        
        // we need to set RETURN to the return value,
        // so that allowReturn can be handled while popping actuals
        if (isMethodCall) { // method call
          s = pushReturnValue(mdecl, s, srcOp);
        } else if (s.isValid()) { // constructor call
          s = lattice.opGet(s, srcOp, StoreLattice.getStackTop(s) - numArgs);
        }
        s = lattice.opSet(s, srcOp, RETURN_VAR);
         
        /* If the call is "super(...)" and the flow unit is a constructor
         * from a nested class, then we have to copy the IPQR to the IFQR.
         */
        if (ConstructorCall.prototype.includes(node) &&
            SuperExpression.prototype.includes(ConstructorCall.getObject(node))) {
          final IRNode enclosingType = VisitUtil.getEnclosingType(flowUnit);
          if (ConstructorDeclaration.prototype.includes(flowUnit) &&
              TypeUtil.isNested(enclosingType)) {
            final IRNode ipqr = JavaPromise.getQualifiedReceiverNodeOrNull(flowUnit);
            final IRNode ifqr = JavaPromise.getQualifiedReceiverNodeOrNull(enclosingType);
            s = lattice.opGet(s, srcOp, ipqr); // read from the parameter
            if (UniquenessRules.isBorrowed(ifqr)) {
              s = lattice.opReturn(s, srcOp, ifqr);
              s = lattice.opRelease(s, srcOp);
            } else {
              /* Pretty sure this message is useless because the IFQR can never
               * be unique
               */
              s = lattice.opCompromise(s, srcOp, Messages.COMPROMISED_BY_IFQR);
            }
          }
        }
              
        // We have to possibly compromise arguments
        if (numArgs > 0) {
          s = popArguments(numArgs, actuals, formals, s);
        }
        if (outerSpec != null) {
          if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Popping qualifier");
          }
          final IRNode outerObject = OuterObjectSpecifier.getObject(outerSpec);
          
          if (!s.isValid()) return s;
          // XXX: This opGet() calls makes errors on outer object references get error checked differently than the receiver and the other args because they do not use an opGet().
          /* Handle value under top: (1) copy it onto top; (2) compromise
           * new top and discard it; (3) popSecond
           */
          s = lattice.opGet(s, outerObject, StoreLattice.getUnderTop(s));
          
          s = popQualifiedReceiver(mdecl, s, outerObject);
          if (!s.isValid()) return s;
          /* This does a popSecond by popping the top value off the stack (op set)
           * and changing the variable just under the top to have the value that 
           * was on the top of the stack.
           */
          s = lattice.opSet(s, srcOp, StoreLattice.getUnderTop(s));
        }
    
        // for method calls (need return value)
        // for new expressions (object already duplicated)
        // and for constructor calls (also already duplicated)
        // we pop the receiver.
        s = popReceiver(mdecl, s, (receiverNode == null) ? srcOp : receiverNode);
      } finally {
        lattice.setSuppressDrops(false);
      }
       
      if (LOG.isLoggable(Level.FINE)) {
        LOG.fine("After handling receivers/qualifiers/parameter: " + s);
      }
      if (isNormal) { // call succeeded
    	  s = lattice.opGet(s, srcOp, RETURN_VAR);
    	  // kill return value
    	  s = lattice.opNull(s);
    	  s = lattice.opSet(s, srcOp, RETURN_VAR);
        return s;
      } else {
        lattice.setAbruptResults(true);
        try {
      	  // kill return value
      	  s = lattice.opNull(s);
      	  s = lattice.opSet(s, srcOp, RETURN_VAR);
          // we pop all pending
          s = popAllPending(s, srcOp);
        } finally {
          lattice.setAbruptResults(false);
        }
        return s;
      }
    }
  
    @Override
  	protected Store transferCast(IRNode node, Store value) {
      	if (lattice.isValueNode(node)) {
      		// force as a value
      		//NB: we don't change the aliases.
      		return lattice.opValue(lattice.opRelease(value, node), node);
      	}
      	return value; // no change;
  	}
  
  	@Override
  	protected Store transferClassExpression(IRNode node, Store val) {
  		return lattice.opValue(val, node);
  	}
  
  	@Override
    protected Store transferCloseScope(final IRNode node, boolean flag, Store s) {
    	/*
    	 * Nullify all variables that were in scope. NB: "undefined" would be closer
    	 * in semantics, but this is not done to check errors, but rather for
    	 * efficiency and null is much cheap in the semantics than undefined. Java's
    	 * scope rules ensure that we don't.
    	 */
    	List<IRNode> toRemove = new ArrayList<IRNode>();
    	for (final IRNode stmt : BlockStatement.getStmtIterator(node)) {
    		if (DeclStatement.prototype.includes(stmt)) {
    			final IRNode vars = DeclStatement.getVars(stmt);
    			for (final IRNode var : VariableDeclarators.getVarIterator(vars)) {
    				toRemove.add(var);
    			}
    		}
    	}
    	return lattice.opClear(s, node, toRemove.toArray());
    }

    @Override
    protected Store transferCatchOpen(final IRNode node, Store s) {
    	IRNode var = CatchClause.getParam(node);
    	s = lattice.opExisting(s, node, State.SHARED, var);
    	return lattice.opSet(s, node, var);
    }
    
    @Override
    protected Store transferCatchClose(final IRNode node, boolean flag, Store s) {
    	IRNode var = CatchClause.getParam(node);
    	// System.out.println("before catch close: " + lattice.toString(s));
    	s = lattice.opClear(s,var);
    	// System.out.println("after catch close: " + lattice.toString(s));
    	return s;
    }
    
    @Override
    protected Store transferDefaultInit(final IRNode node, final Store s) {
      // reference types initialized with null
      // other types are primitive and hence null (for uniqueness analysis)
      return lattice.opNull(s);
    }

    @Override
    protected Store transferEnumConstantDeclarationAsFieldInit(final IRNode node,  Store s) {
      /* N.B. Should be handled as a special case of field initialization.
       * See transferInitializationOfField(), below.
       */
      
      // Static field init, so we push the shared object onto the stack 
      s = lattice.opExisting(s, node, State.SHARED, null);
      s = lattice.opDup(s, node, 1); // get value on top of stack
      s = lattice.opStore(s, node, node); // perform store
      
      /* Regular field init does opRelease.  But enum constants are shared
       * because they are stored into an elements array by the enum
       * implementation.  So we have to compromise the value being stored
       * into the enum constant.  (That is, we can never assure a @Unique
       * enum constant.) 
       */
      s = lattice.opCompromise(s, node, Messages.COMPROMISED_BY_FIELD_ASSIGNMENT,
          EnumConstantDeclaration.getId(node));
      return s;
    }

    @Override
    protected Store transferEnumConstantDeclarationAsAnonClass(final IRNode node,  Store s) {
      /* N.B. Should be handled as a special case of anonymous class
       */
      return transferNestedClassUse(node,  s);
    }

    @Override
    protected Store transferEq(final IRNode node, final boolean flag, final Store s) {
      // compare top two stack values and pop off.
      // then push a boolean value (unused).
      return lattice.opValue(lattice.opEqual(s, node, flag), node);
    }
    
    @Override
    protected Store transferFailedCall(final IRNode node, final Store s) {
      throw new FluidError("execution should not reach here.");
    }

    @Override
    protected Store transferImpliedNewExpression(
        final IRNode node, final boolean flag, final Store s) {
      // N.B. Should be handled as a specialized case of transferCall
      final IRNode enumConst = JJNode.tree.getParent(node);
      final Operator enumConstOp = JJNode.tree.getOperator(enumConst);
      final IRNode actuals;
      if (SimpleEnumConstantDeclaration.prototype.includes(enumConstOp)) {
        actuals = null;
      } else if (NormalEnumConstantDeclaration.prototype.includes(enumConstOp)) {
        actuals = NormalEnumConstantDeclaration.getArgs(enumConst);
      } else { // EnumConstantClassConstantDeclaration
        actuals = EnumConstantClassDeclaration.getArgs(enumConst);
      }

      return handleCallLikeExpressions(
          enumConst, flag, s, enumConst, binder.getBinding(enumConst),
          actuals, null, false, null);
    }

    @Override
    protected Store transferInitializationOfField(final IRNode node, Store s) {
      final boolean fineIsLoggable = LOG.isLoggable(Level.FINE);
      // get class or "this" on stack
      if (TypeUtil.isStatic(node)) {
        s = lattice.opExisting(s, node, State.SHARED, null);
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
        if (fineIsLoggable) {
          LOG.fine("initializing field '" + JJNode.getInfo(node) + "'");
        }
      }
      s = lattice.opDup(s, node, 1); // get value on top of stack
      s = lattice.opStore(s, node, node); // perform store
      s = lattice.opRelease(s, node); // throw away the extra copy
      return s;
    }
    
    @Override
    protected Store transferInitializationOfVar(final IRNode node, final Store s) {
      return lattice.opSetAliasAware(s, node, node);
//      return lattice.opSet(s, node, node);
    }
    
    @Override
    protected Store transferLiteral(final IRNode node, final Store s) {
      final Operator op = tree.getOperator(node);
      if (NullLiteral.prototype.includes(op)) {
        return lattice.opNull(s);
      } else if (RefLiteral.prototype.includes(op)) {
  		if (StringLiteral.prototype.includes(op) &&
  				JJNode.getInfo(node).startsWith("\"Unique")) {
			// debugging hook
			System.out.println("At " + JJNode.getInfo(node) + ": " + lattice.toString(s));
		}
        return lattice.opValue(s, node); // push a shared String constant
      } else {
        return lattice.opNull(s); // push nothing 
      }
    }
    
    /* Original override transferIntialization(), but there is no reason to. */
    
    @Override
    protected Store transferMethodBody(final IRNode body, final Port kind, Store s) {
      if (kind instanceof EntryPort) {
        return s; // opStart() was invoked when the flow unit was entered
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
          s = lattice.opGet(s, body, rnode, BuriedMessage.RETURN);
          s = lattice.opRemoveFrom(s, rnode);
          if (lattice.isValueNode(rnode)) {
        	  // no check necessary: Java type system handles
        	  s = lattice.opRelease(s, body);
          } else {
            /* Here we may pass SHARED or UNIQUE to opConsume, which we 
             * normally shouldn't do, but this happens after there is anything
             * to detect the compromising or undefining, respectively, of the
             * return value.
             */
        	  s = lattice.opConsume(s, rnode, lattice.declStatus(rnode));
          }
          if (fineIsLoggable) {
            LOG.fine("After handling return value of " + name + ": " + s);
          }
          s = lattice.opStop(s, body);
        } else if (kind instanceof NormalExitPort && 
        		ConstructorDeclaration.prototype.includes(mOp)) {
        	final IRNode rnode = JavaPromise.getReceiverNode(mdecl);
        	s = lattice.opRemoveFrom(s,rnode);
          s = lattice.opStop(s, body);
        } else if (kind instanceof AbruptExitPort) {
          lattice.setAbruptResults(true);
          try {
            s = lattice.opStop(s, body);
          } finally {
            lattice.setAbruptResults(false);
          }
        } else {
          // SHouldn't get here??? Stop anyway
          s = lattice.opStop(s, body);
        }
        if (fineIsLoggable) {
          LOG.fine("At end of " + name + ": " + s);
        }
        return s;
      }
    }
    
    /**
     * Transfer over the closure of a nested class over local variables.
     * This happens at creation time (not declaration elaboration).
     * @param node AnonClassExpression or NestedClassDeclaration
     * @param s store before
     * @return store after
     */
    protected Store transferNestedClassUse(IRNode node, Store s) {
    	// TODO: add "from" ideas.
    	// Complications:
    	// (1) We need to find the IFQR for the anonymous class
    	//     If borrowed, we avoid compromising things, and instead add *-edges
    	// (2) We need to check if the IFQR is used (if not, we avoid adding an *-edge from this)
    	// (3) Then we check each parameter and add *-edges for non-shared things.
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
    	// TODO: Here we compromise "this" if it is used
    	// We should do that if the qualified receiver is not borrowed.
    	// If the qualified receiver is readonly, we should only compromise at the read level.
    	/*
    	IRNode fqr = getQualifiedReceiver(node);
    	boolean isBorrowed = UniquenessRules.isBorrowed(fqr);
    	*/
      final IRNode classBody = VisitUtil.getClassBody(node);        
    	// called on NestedClassDeclaration AND AnonClassExpression
      final List<IRNode> externalVars =
          LocalVariableDeclarations.getExternallyDeclaredVariables(
              JavaPromise.getInitMethodOrNull(node));
      final Set<IRNode> usedExternalVars = new HashSet<IRNode>();
      boolean usesExternal = false;
  	  for (final IRNode n : tree.bottomUp(classBody)) {
        if (VariableUseExpression.prototype.includes(n)) {
          final IRNode decl = binder.getBinding(n);
          if (externalVars.contains(decl)) {
            usesExternal = true;
            usedExternalVars.add(decl);
//       		  //XXX: Here we compromise.  If we want to
//       		  // be less conservative than this (and this will give errors
//       		  // if the value is ReadOnly), we need to use both the
//       		  // possible borrowed-ness of the local FQR, but also
//       		  // need an annotation on the local specific to the NCD or ACE.
//            
//            // TODO: DOn't use 'n' here.
//       		  s = lattice.opCompromise(lattice.opGet(s, n, decl));                	
          }
        } else if (QualifiedThisExpression.prototype.includes(n)) {
         	usesExternal = true;
        }
      }
          
  	  // Compromise the external variables that are used
  	  for (final IRNode v : usedExternalVars) {
  	    s = lattice.opCompromise(
  	        lattice.opGet(
  	            s, node, v, BuriedMessage.EXTERNAL_VAR), node,
  	            Messages.COMPROMISED_BY_HIDDEN_FIELD, VariableDeclaration.getId(v));
  	  }
  	  
      /* If we used outer things and we aren't in a static context then
       * compromise "this" of the method that contains the nested class, because
       * that is the only reference that can be the actual outer object.
       */
      final IRNode rcvr = JavaPromise.getReceiverNodeOrNull(flowUnit);
      if (usesExternal && rcvr != null) { 
        // Now compromise "this" (this is slightly more conservative than necessary)
        s = lattice.opCompromise(lattice.opGet(s, node, rcvr), node,
            Messages.COMPROMISED_BY_OUTER_OBJECT);
      }
      return s;
	  }



	@Override
    protected Store transferRelop(IRNode node, Operator op, boolean flag, Store value) {
    	if (EqualityExpression.prototype.includes(op)) {
    		return super.transferRelop(node, op, flag, value);
    	} else if (CompareExpression.prototype.includes(op)) {
    		return lattice.opNull(pop(pop(value, node), node));
    	} else if (InstanceOfExpression.prototype.includes(op)) {
    		return lattice.opNull(pop(value, node));
    	}
    	// ERROR:
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
        IRNode returnNode = JavaPromise.getReturnNode(node);
		return lattice.opSet(lattice.opReturn(s, node, returnNode), node, returnNode);
      }
    }
    
    @Override
    protected Store transferThrow(final IRNode node, final Store s) {
      return lattice.opCompromise(s, node, Messages.COMPROMISED_BY_THROW);
    }
    
	@Override
	protected Store transferType(IRNode node, Store val) {
		return lattice.opGenerate(val, node, State.SHARED, node);
	}

	@Override
	protected Store transferUnop(IRNode node, Operator op, Object info, Store val) {
    	if (ArithUnopExpression.prototype.includes(op)) {
    		return lattice.opNull(pop(val, node));
    	} else if (ComplementExpression.prototype.includes(op)) {
    		return lattice.opNull(pop(val, node));
    	} else if (UnboxExpression.prototype.includes(op)) {
    		return lattice.opNull(pop(val, node));
    	} else if (BoxExpression.prototype.includes(op)) {
    		return lattice.opValue(pop(val, node), node);
    	}
		return super.transferUnop(node, op, info, val);
	}

	@Override
    protected Store transferUseArray(final IRNode aref, Store s) {
      if (!s.isValid()) return s;
      if (isBothLhsRhs(aref)) {
        s = dup(dup(s, aref), aref);
      }
      s = lattice.opRelease(s, aref);
      s = lattice.opRelease(s, aref);
      // If we knew something about the array, we could do better:
      s = lattice.opExisting(s, aref, State.SHARED, aref);
      return s;
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
        s = pop(s, fref);
        return lattice.opNull(s); // just put null on stack (least likely to cause additional errors)
      } else {
        return lattice.opLoad(s, fref, decl);
      }
    }
    
    @Override
    protected Store transferUseArrayLength(final IRNode alen, Store s) {
      if (!s.isValid()) return s;
      s = lattice.opRelease(s, alen);
      return lattice.opNull(s); // push a non-pointer value (a primitive integer in this case)
    }
    
    @Override
    protected Store transferUseVar(final IRNode var, final Store s) {
      final IRNode decl = binder.getBinding(var);
      if (decl == null) {
        LOG.warning("Cannot find binding for " + DebugUnparser.toString(var));
        return lattice.opNull(s);
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
      /* If the qualified receiver is an implicit parameter of a constructor
       * then we handle it as a local variable.  Otherwise it is series of
       * field loads.
       */
      if (ConstructorDeclaration.prototype.includes(
          JavaPromise.getPromisedFor(decl))) { // constructor parameter
        return lattice.opGet(s, use, decl);
      } else {
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
    
    private IRNode effectformal(Effect x) {
    	Target t = x.getTarget();
    	if (t instanceof InstanceTarget) {
    		IRNode n = t.getReference();
    		Operator op = JJNode.tree.getOperator(n);
    		if (op instanceof ReceiverDeclaration || 
    				op instanceof ParameterDeclaration) return n;
    	}
    	return null;
    }
    
    @SuppressWarnings("unused")
	private void debugEffects(IRNode n) {
    	List<Effect> fx = Effects.getDeclaredMethodEffects(n, n);
    	System.out.println(DebugUnparser.toString(n));
    	if (fx == null) {
    		System.out.println("No declared effects");
    		return;
    	}
    	for (Effect f1 : fx) {
    		IRNode n1 = effectformal(f1);
    		if (n1 == null) continue;
    		IRegion r1 = f1.getTarget().getRegion();
    		for (Effect f2 : fx) {
    			if (f1 != f2 && (f1.isWrite() || f2.isWrite())) {
    				IRNode n2 = effectformal(f2);
    				if (n2 == null) continue;
    				IRegion r2 = f2.getTarget().getRegion();
    				// if the regions overlap, then we can say that n1 and n2 are not aliases
    				if (r1.overlapsWith(r2)) {
    					System.out.println("Found effect-based non-alias: " + 
    							DebugUnparser.toString(n1) + " != " +
    							DebugUnparser.toString(n2) + " because " +
    							f1 + " and " + f2);
    				}
    			}
    		}
    	}
    }
    
    
    public Store transferComponentSource(final IRNode node) {
    	// debugEffects(node);
      return lattice.opStart(node);
    }
  }
  
  
  
  // ==================================================================
  // === SubAnalysis factory
  // ==================================================================

  private static final class SubAnalysisFactory extends AbstractCachingSubAnalysisFactory<StoreLattice, Store> {
    private final AtomicBoolean reworkFlag;
    private final IRNode flowUnit;
    private final boolean timeOut;
    
    public SubAnalysisFactory(
        final AtomicBoolean flag, final IRNode fu, final boolean to) {
      reworkFlag = flag;
      flowUnit = fu;
      timeOut = to;
    }
    
    @Override
    protected JavaForwardAnalysis<Store, StoreLattice> realCreateAnalysis(
        final IRNode caller, final IBinder binder, final StoreLattice lattice,
        final Store initialValue, final boolean terminationNormal) {
      final int floor = initialValue.isValid() ? initialValue.getStackSize().intValue() : 0;
      final UniquenessTransfer transfer =
          new UniquenessTransfer(reworkFlag, binder, new Effects(binder), lattice, floor, flowUnit, timeOut);
      return new UniquenessChild(reworkFlag, "Sub Analysis", lattice, transfer, timeOut);
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
