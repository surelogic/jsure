package com.surelogic.analysis.uniqueness;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.analysis.IBinderClient;
import com.surelogic.analysis.LocalVariableDeclarations;
import com.surelogic.analysis.alias.IMayAlias;
import com.surelogic.analysis.alias.TypeBasedMayAlias;
import com.surelogic.analysis.effects.Effect;
import com.surelogic.analysis.effects.Effects;
import com.surelogic.analysis.effects.targets.InstanceTarget;
import com.surelogic.analysis.effects.targets.Target;
import com.surelogic.analysis.regions.IRegion;
import com.surelogic.analysis.uniqueness.store.State;
import com.surelogic.analysis.uniqueness.store.Store;
import com.surelogic.analysis.uniqueness.store.StoreLattice;
import com.surelogic.annotation.rules.UniquenessRules;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.util.IThunk;

import edu.cmu.cs.fluid.FluidError;
import edu.cmu.cs.fluid.control.Component.WhichPort;
import edu.cmu.cs.fluid.control.EntryPort;
import edu.cmu.cs.fluid.control.NormalExitPort;
import edu.cmu.cs.fluid.control.Port;
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
import edu.cmu.cs.fluid.java.operator.AnonClassExpression;
import edu.cmu.cs.fluid.java.operator.ArithUnopExpression;
import edu.cmu.cs.fluid.java.operator.BlockStatement;
import edu.cmu.cs.fluid.java.operator.CallInterface;
import edu.cmu.cs.fluid.java.operator.CatchClause;
import edu.cmu.cs.fluid.java.operator.CompareExpression;
import edu.cmu.cs.fluid.java.operator.ComplementExpression;
import edu.cmu.cs.fluid.java.operator.ConstructorDeclaration;
import edu.cmu.cs.fluid.java.operator.DeclStatement;
import edu.cmu.cs.fluid.java.operator.EqualityExpression;
import edu.cmu.cs.fluid.java.operator.InstanceOfExpression;
import edu.cmu.cs.fluid.java.operator.MethodCall;
import edu.cmu.cs.fluid.java.operator.MethodDeclaration;
import edu.cmu.cs.fluid.java.operator.NestedClassDeclaration;
import edu.cmu.cs.fluid.java.operator.NullLiteral;
import edu.cmu.cs.fluid.java.operator.ParameterDeclaration;
import edu.cmu.cs.fluid.java.operator.RefLiteral;
import edu.cmu.cs.fluid.java.operator.ReferenceType;
import edu.cmu.cs.fluid.java.operator.StringConcat;
import edu.cmu.cs.fluid.java.operator.StringLiteral;
import edu.cmu.cs.fluid.java.operator.UnboxExpression;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.operator.VariableDeclarators;
import edu.cmu.cs.fluid.java.operator.VariableUseExpression;
import edu.cmu.cs.fluid.java.operator.VoidType;
import edu.cmu.cs.fluid.java.promise.QualifiedReceiverDeclaration;
import edu.cmu.cs.fluid.java.promise.ReceiverDeclaration;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.drops.promises.BorrowedPromiseDrop;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.Iteratable;
import edu.uwm.cs.fluid.java.analysis.IntraproceduralAnalysis;
import edu.uwm.cs.fluid.java.control.AbstractCachingSubAnalysisFactory;
import edu.uwm.cs.fluid.java.control.IJavaFlowAnalysis;
import edu.uwm.cs.fluid.java.control.JavaEvaluationTransfer;
import edu.uwm.cs.fluid.java.control.JavaForwardAnalysis;

public final class UniquenessAnalysis extends IntraproceduralAnalysis<Store, StoreLattice, JavaForwardAnalysis<Store, StoreLattice>> implements IBinderClient {
  // ==================================================================
  // === Fields
  // ==================================================================

  private final boolean timeOut;
  private final IMayAlias mayAlias;
  
  // ==================================================================
  // === Constructor 
  // ==================================================================
  
  public UniquenessAnalysis(final IBinder binder, final boolean to) {
    super(new FixBinder(binder)); // avoid crashes.
    mayAlias = new TypeBasedMayAlias(binder);
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
    final IRNode returnNode = JavaPromise.getReturnNodeOrNull(flowUnit);
    if (returnNode != null && LocalVariableDeclarations.hasReferenceType(binder, returnNode)) {
      refLocals.add(returnNode);
    }

    // Add the receiver & any qualified receivers
    final IRNode rcvrNode = JavaPromise.getReceiverNodeOrNull(flowUnit);
    if (rcvrNode != null) {
      refLocals.add(rcvrNode);
    }
    for (final IRNode qrcvr : JavaPromise.getQualifiedReceiverNodes(flowUnit)) {
      refLocals.add(qrcvr);
    }
    
    final IRNode[] locals = refLocals.toArray(new IRNode[refLocals.size()]);
    Set<Effect> effects = Effects.getDeclaredMethodEffects(flowUnit, flowUnit);
	final StoreLattice lattice = new StoreLattice(locals,mayAlias,effects);
    return new Uniqueness(
        "Uniqueness Analsys (UWM)", lattice,
        new UniquenessTransfer(binder, mayAlias, lattice, 0, flowUnit, timeOut),
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

    public IBinding getIBinding(IRNode node) {
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
  // === Flow Analysis: Subclass to throw Gave up exception
  // ==================================================================

  private static final class Uniqueness extends JavaForwardAnalysis<Store, StoreLattice> {
    public Uniqueness(
        final String name, final StoreLattice lattice,
        final UniquenessTransfer transfer, boolean timeOut) {
      super(name, lattice, transfer, DebugUnparser.viewer, timeOut);
    }
  }
  
  
  
  // ==================================================================
  // === Transfer Function 
  // ==================================================================

  private static final class UniquenessTransfer extends JavaEvaluationTransfer<StoreLattice, Store> {
    /** Logger instance for debugging. */
    private static final Logger LOG = SLLogger
        .getLogger("FLUID.analysis.unique.transfer");

    private final IRNode flowUnit;

    private final IMayAlias mayAlias;
    private final Effects effects;
    
    
    // ==================================================================
    // === Constructor 
    // ==================================================================

    public UniquenessTransfer(final IBinder binder,
        final IMayAlias ma, final StoreLattice lattice,
        final int floor, final IRNode fu, final boolean timeOut) {
      super(binder, lattice, new SubAnalysisFactory(fu, ma, timeOut), floor);
      flowUnit = fu;
      mayAlias = ma;
      effects = new Effects(binder);
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
        final int numFormals, final IRNode formals,
        final Set<Effect> declEffects, Store s) {
      if (!s.isValid()) return s;
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
        if (actualStackDepth == null)
        	s = lattice.opExisting(s, State.SHARED);
        else
        	s = lattice.opDup(s, actualStackDepth); 
      
        final IRegion r = t.getRegion();
        if (r.isAbstract()) {
          // it could refer to almost anything
          // everything reachable from this pointer is alias buried:
          s = lattice.opRelease(lattice.opLoadReachable(s));
        } else {
          // it's a field, load and discard.
          s = lattice.pop(lattice.opLoad(s, r.getNode()));
        }
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
    			return numFormals - i - 1;  // was + 1; fixed 2011-01-07
    		}
    	}

    	return null;
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
    	return lattice.opFrom(s, lattice.getStackTop(s), RETURN_VAR);
    }

    /**
     * Return the store after popping off and processing each actual parameter
     * according to the formal parameters. The number should be the same (or else
     * how did the binder determine to call this method/constructor?).
     * The formals may be null only if there is some sort of error.
     * This error is logged as a warning already.
     */
    private Store popArguments(
        final int numActuals, final IRNode formals, Store s) {
      if (formals != null && numActuals != tree.numChildren(formals)) {
        throw new FluidError("#formals != #actuals");
      }
      for (int n = numActuals - 1; n >= 0; n--) {
        final IRNode formal = formals != null ? tree.getChild(formals, n) : null;
        if (formal != null && UniquenessRules.isUnique(formal)) {
          s = lattice.opUndefine(s);
        } else if (formal == null || UniquenessRules.isBorrowed(formal)) {
        	if (UniquenessRules.getBorrowed(formal).allowReturn()) {
        		s = addFromNode(s);
        		System.out.println("Popping a borrowed(allowReturn) actual, state is " + lattice.toString(s));
        	}
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
    private Store popReceiver(final IRNode decl, Store s) {
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
        	BorrowedPromiseDrop borrowed = UniquenessRules.getBorrowed(retDecl);
			if (borrowed != null && borrowed.allowReturn()) {
				s = addFromNode(s);
        		System.out.println("Found Borrowed(allowReturn) receiver; state is " + lattice.toString(s));
        	}
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
     * The top of the stack was a receiver of a inner class constructor call, pop it off using one
     * of the three methods for popping depending on the promises/demands about
     * the qualified receiver.
     */
    private Store popQualifiedReceiver(final IRNode decl, Store s) {
      if (decl == null) {
        return lattice.opCompromise(s);
      }
      System.out.println(DebugUnparser.toString(decl));
      IRNode p = JJNode.tree.getParent(decl);
      while (p != null && !(JJNode.tree.getOperator(p) instanceof NestedClassDeclaration))
    	  p = JJNode.tree.getParent(p);
      IRNode qr = JavaPromise.getQualifiedReceiverNodeByName(decl, p);
      if (UniquenessRules.isBorrowed(qr)) {
    	  System.out.println("Qualified receiver is borrowed.");
    	  BorrowedPromiseDrop borrowed = UniquenessRules.getBorrowed(qr);
			if (borrowed != null && borrowed.allowReturn()) {
				s = addFromNode(s);
      		System.out.println("Found Borrowed(allowReturn) qualified receiver, now " + lattice.toString(s));
      	}
        return lattice.opBorrow(s);      
      } else {
    	  System.out.println("Qualified receiver is NOT borrowed");
    	  return lattice.opCompromise(s);
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
      // Will crash if given a ConstructorDeclaration
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
    protected Store transferAllocation(final IRNode node, final Store s) {
      return lattice.opNew(s);
    }
    
    @Override
    protected Store transferAnonClass(final IRNode node, Store s) {
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
            s = lattice.opCompromise(lattice.opGet(s, decl));
          }
        }
      }
      
      // If we aren't in a static context then compromise "this"
      if (JavaPromise.getReceiverNodeOrNull(flowUnit) != null) { 
        // Now compromise "this" (this is slightly more conservative than necessary)
        s = lattice.opCompromise(lattice.opThis(s));
      }
      return s;
    }
    
    @Override
    protected Store transferArrayCreation(final IRNode node, final Store s) {
      // inefficient but simple
      return lattice.opNew(lattice.pop(super.transferArrayCreation(node, s)));
    }
    
    @Override
    protected Store transferArrayInitializer(final IRNode node, final Store s) {
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
	protected Store transferBinop(IRNode node, Operator op, Store val) {
    	if (op instanceof StringConcat)
    		return super.transferBinop(node, op, val);
    	else {
    		return lattice.push(lattice.pop(lattice.pop(val)));
    	}
	}


	@Override
    protected Store transferCall(final IRNode node, final boolean flag, Store s) {
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
        if (ConstructorDeclaration.prototype.includes(mdecl)) {
          formals = ConstructorDeclaration.getParams(mdecl);
        } else {
          formals = MethodDeclaration.getParams(mdecl);
        }
      }
//      final IRNode receiverNode = mcall ? ((MethodCall) call).get_Object(node) : null;
      if (mdecl != null) {
//        /* If the flowunit is a class body then we are dealing with instance 
//         * initialization and the calls is the <init> method.
//         */
//        final IRNode caller;
//        if (ClassBody.prototype.includes(flowUnit)) {
//          caller = JavaPromise.getInitMethod(JJNode.tree.getParent(flowUnit));
//        } else {
//          caller = flowUnit;
//        }
//        s = considerEffects(receiverNode, numActuals, actuals,
//            effects.getMethodCallEffects(null, node, caller, true), s);
        s = considerDeclaredEffects(numActuals, formals,
            effects.getMethodEffects(mdecl, node), s);
      }
      
      // we need to set RETURN to the return value,
      // so that allowReturn can be handled while popping actuals
      if (mcall) {
    	  s = pushReturnValue(mdecl,s);
      } else if (s.isValid()){
    	  s = lattice.opGet(s,lattice.getStackTop(s)-numActuals);
      }
      s = lattice.opSet(s, RETURN_VAR);
      
      // We have to possibly compromise arguments
      s = popArguments(numActuals, formals, s);
      if (hasOuterObject(node)) {
    	  System.out.println("Popping qualifier");
        if (LOG.isLoggable(Level.FINE)) {
          LOG.fine("Popping qualifier");
        }
        if (!s.isValid()) return s;
        /* Compromise value under top: (1) copy it onto top; (2) compromise
         * new top and discard it; (3) popSecond
         */
        s = lattice.opGet(s, lattice.getUnderTop(s));
        
        s = popQualifiedReceiver(mdecl,s);
        if (!s.isValid()) return s;
        /* This does a popSecond by popping the top value off the stack (op set)
         * and changing the variable just under the top to have the value that 
         * was on the top of the stack.
         */
        s = lattice.opSet(s, lattice.getUnderTop(s));
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
    	  s = lattice.opGet(s, RETURN_VAR);
    	  // kill return value
    	  s = lattice.opNull(s);
    	  s = lattice.opSet(s,RETURN_VAR);
        return s;
      } else {
    	  // kill return value
    	  s = lattice.opNull(s);
    	  s = lattice.opSet(s,RETURN_VAR);
        // we pop all pending
        s = popAllPending(s);
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
    	List<IRNode> toRemove = new ArrayList<IRNode>();
    	for (final IRNode stmt : BlockStatement.getStmtIterator(node)) {
    		if (DeclStatement.prototype.includes(stmt)) {
    			final IRNode vars = DeclStatement.getVars(stmt);
    			for (final IRNode var : VariableDeclarators.getVarIterator(vars)) {
    				toRemove.add(var);
    			}
    		}
    	}
    	return lattice.opClear(s,toRemove.toArray());
    }

    @Override
    protected Store transferCatchOpen(final IRNode node, Store s) {
    	IRNode var = CatchClause.getParam(node);
    	s = lattice.opExistingBetter(s, State.SHARED, mayAlias, var);
    	return lattice.opSet(s, var);
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
  		if (StringLiteral.prototype.includes(op) &&
  				((String)(JJNode.getInfo(node))).startsWith("\"Unique")) {
			// debugging hook
			System.out.println("At " + JJNode.getInfo(node) + ": " + lattice.toString(s));
		}
        return push(s); // push a shared String constant
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
        if (kind instanceof NormalExitPort &&
            MethodDeclaration.prototype.includes(mOp) && 
            !VoidType.prototype.includes(MethodDeclaration.getReturnType(mdecl))) {
          final IRNode rnode = JavaPromise.getReturnNode(mdecl);
          s = lattice.opGet(s, rnode);
          s = lattice.opRemoveFrom(s, rnode);
          if (UniquenessRules.isUnique(rnode)) {
            s = lattice.opUndefine(s);
          } else {
            s = lattice.opCompromise(s);
          }
          if (fineIsLoggable) {
            LOG.fine("After handling return value of " + name + ": " + s);
          }
        } else if (kind instanceof NormalExitPort && 
        		ConstructorDeclaration.prototype.includes(mOp)) {
        	final IRNode rnode = JavaPromise.getReceiverNode(mdecl);
        	s = lattice.opRemoveFrom(s,rnode);
        }
        s = lattice.opStop(s);
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
    		return lattice.push(lattice.pop(lattice.pop(value)));
    	} else if (InstanceOfExpression.prototype.includes(op)) {
    		return lattice.push(lattice.pop(value));
    	}
    	return super.transferRelop(node, op, flag, value);
    }

	@Override
    protected Store transferReturn(IRNode node, final Store s) {
      while (node != null && !MethodDeclaration.prototype.includes(node)) {
        node = tree.getParentOrNull(node);
      }
      if (node == null) {
        return pop(s);
      } else {
        IRNode returnNode = JavaPromise.getReturnNode(node);
		return lattice.opSet(lattice.opReturn(s,returnNode), returnNode);
      }
    }
    
    @Override
    protected Store transferThrow(final IRNode node, final Store s) {
      return lattice.opCompromise(s);
    }
    
    @Override
	protected Store transferUnop(IRNode node, Operator op, Object info, Store val) {
    	if (ArithUnopExpression.prototype.includes(op)) {
    		return lattice.push(lattice.pop(val));
    	} else if (ComplementExpression.prototype.includes(op)) {
    		return lattice.push(lattice.pop(val));
    	} else if (UnboxExpression.prototype.includes(op)) {
    		return lattice.push(lattice.pop(val));
    	}
		return super.transferUnop(node, op, info, val);
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
    	Set<Effect> fx = Effects.getDeclaredMethodEffects(n, n);
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
      return lattice.opStart();
    }
  }
  
  
  
  // ==================================================================
  // === SubAnalysis factory
  // ==================================================================

  private static final class SubAnalysisFactory extends AbstractCachingSubAnalysisFactory<StoreLattice, Store> {
    private final IRNode flowUnit;
    private final boolean timeOut;
    private final IMayAlias mayAlias;
    
    public SubAnalysisFactory(final IRNode fu, final IMayAlias ma, final boolean to) {
      flowUnit = fu;
      mayAlias = ma;
      timeOut = to;
    }
    
    @Override
    protected JavaForwardAnalysis<Store, StoreLattice> realCreateAnalysis(
        final IRNode caller, final IBinder binder, final StoreLattice lattice,
        final Store initialValue, final boolean terminationNormal) {
      final int floor = initialValue.isValid() ? initialValue.getStackSize().intValue() : 0;
      final UniquenessTransfer transfer = new UniquenessTransfer(binder, mayAlias, lattice, floor, flowUnit, timeOut);
      return new Uniqueness("Sub Analysis", lattice, transfer, timeOut);
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
}
