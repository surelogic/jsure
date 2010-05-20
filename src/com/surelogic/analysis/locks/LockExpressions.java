package com.surelogic.analysis.locks;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import com.surelogic.analysis.AbstractThisExpressionBinder;
import com.surelogic.analysis.bca.uwm.BindingContextAnalysis;
import com.surelogic.analysis.locks.LockUtils.HowToProcessLocks;
import com.surelogic.analysis.locks.locks.HeldLock;
import com.surelogic.analysis.locks.locks.HeldLockFactory;
import com.surelogic.analysis.messages.Messages;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaDeclaredType;
import edu.cmu.cs.fluid.java.bind.JavaTypeFactory;
import edu.cmu.cs.fluid.java.operator.AnonClassExpression;
import edu.cmu.cs.fluid.java.operator.ClassBody;
import edu.cmu.cs.fluid.java.operator.ClassInitializer;
import edu.cmu.cs.fluid.java.operator.ConstructorCall;
import edu.cmu.cs.fluid.java.operator.FieldDeclaration;
import edu.cmu.cs.fluid.java.operator.MethodCall;
import edu.cmu.cs.fluid.java.operator.SuperExpression;
import edu.cmu.cs.fluid.java.operator.SynchronizedStatement;
import edu.cmu.cs.fluid.java.operator.TypeDeclaration;
import edu.cmu.cs.fluid.java.operator.VoidTreeWalkVisitor;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.drops.effects.RegionEffectsPromiseDrop;
import edu.cmu.cs.fluid.sea.drops.promises.BorrowedPromiseDrop;
import edu.cmu.cs.fluid.sea.drops.promises.StartsPromiseDrop;
import edu.cmu.cs.fluid.sea.drops.promises.UniquePromiseDrop;
import edu.cmu.cs.fluid.sea.proxy.ResultDropBuilder;
import edu.cmu.cs.fluid.tree.Operator;

/**
 * A record of the lock expressions used in a method/constructor.  Specifically,
 * keeps separate track of
 * <ul>
 * <li>All the syntactically unique final lock expressions that appear as the
 * object expression in a call to {@code lock()} or {@code unlock()}.
 * <li>All the syntactically unique final lock expressions that appear the
 * argument to a {@code synchronized} block.
 * <li>The set of JUC locks that are declared as preconditions.
 * <li>The set of intrinsic locks that are declared as preconditions.
 * <li>The set of JUC locks that are held if the constructor is single threaded.
 * <li>The set of intrinsic locks that are held if the constructor is single threaded.
 * <li>The set of static JUC locks that are held because we are analyzing the
 * class initializer.
 * <li>The set of static intrinsic locks that are held because we are analyzing the
 * class initializer.
 * </ul> 
 */
final class LockExpressions {
  private final static class TEB extends AbstractThisExpressionBinder {
    /* non-private so that LockExpressionVisitor can access this field
     * instead of maintaining a separate copy of it. 
     */
    IRNode enclosingFlowUnit;
    private final LinkedList<IRNode> declStack = new LinkedList<IRNode>();
    private IRNode currentRcvr;
    private final LinkedList<IRNode> rcvrStack = new LinkedList<IRNode>();
    
    public TEB(final IBinder b, final IRNode mdecl) {
      super(b);
      enclosingFlowUnit = mdecl;
      currentRcvr = JavaPromise.getReceiverNodeOrNull(mdecl);
    }

    @Override
    protected IRNode bindReceiver(final IRNode node) {
      return currentRcvr;
    }

    @Override
    protected IRNode bindQualifiedReceiver(
        final IRNode outerType, final IRNode node) {
      return JavaPromise.getQualifiedReceiverNodeByName(enclosingFlowUnit, outerType);
    }
    
    public void newDeclaration(final IRNode decl) {
      declStack.addFirst(enclosingFlowUnit);
      rcvrStack.addFirst(currentRcvr);
      enclosingFlowUnit = decl;
      currentRcvr = JavaPromise.getReceiverNodeOrNull(decl);
    }
    
    public void pop() {
      enclosingFlowUnit = declStack.removeFirst();
      currentRcvr = rcvrStack.removeFirst();
    }    
  }

  
  
  public final static class SingleThreadedData {
    public final boolean isBorrowedThis;
    public final BorrowedPromiseDrop bDrop;

    public final boolean isUniqueReturn;
    public final UniquePromiseDrop uDrop;
    
    public final boolean isEffects;
    public final RegionEffectsPromiseDrop eDrop;
    public final StartsPromiseDrop teDrop;
    
    public final boolean isSingleThreaded;
    
    public SingleThreadedData(
        final boolean isBorrowedThis, final BorrowedPromiseDrop bDrop,
        final boolean isUniqueReturn, final UniquePromiseDrop uDrop,
        final boolean isEffects,
        final RegionEffectsPromiseDrop eDrop, final StartsPromiseDrop teDrop) {
      this.isBorrowedThis = isBorrowedThis;
      this.bDrop = bDrop;
      this.isUniqueReturn = isUniqueReturn;
      this.uDrop = uDrop;
      this.isEffects = isEffects;
      this.eDrop = eDrop;
      this.teDrop = teDrop;
      this.isSingleThreaded = isUniqueReturn || isBorrowedThis || isEffects;
    }
    
    public void addSingleThreadedEvidence(final ResultDropBuilder result) {
      if (isUniqueReturn) {
        result.addTrustedPromise_or(Messages.LockAnalysis_ds_SingleThreadedUniqueReturn, uDrop);
      }
      if (isBorrowedThis) {
        result.addTrustedPromise_or(Messages.LockAnalysis_ds_SingleThreadedBorrowedThis, bDrop);
      }
      if (isEffects) {
        // Note: "by effects" has to be the same string to "and" the "or"
        result.addTrustedPromise_or(Messages.LockAnalysis_ds_SingleThreadedEffects, eDrop);
        result.addTrustedPromise_or(Messages.LockAnalysis_ds_SingleThreadedEffects, teDrop);
      }
    }
  }
  
  /**
   * The declaration of the method/constructor being analyzed
   */
  private final IRNode enclosingMethodDecl;
  
  /**
   * Map from final lock expressions used as the object expressions to lock() or
   * unlock() calls to the JUC locks they resolve to.
   */
  private final Map<IRNode, Set<HeldLock>> jucLockExprsToLockSets =
    new HashMap<IRNode, Set<HeldLock>>();

  /** Any JUC locks that are declared in a lock precondition */
  private final Set<HeldLock> jucRequiredLocks = new HashSet<HeldLock>();

  /**
   * The JUC locks that apply because we are analyzing a singled-threaded 
   * constructor.
   */
  private final Set<HeldLock> jucSingleThreaded = new HashSet<HeldLock>();

  /**
   * The JUC locks that apply because we are analyzing the class initializer.
   */
  private final Set<HeldLock> jucClassInit = new HashSet<HeldLock>();
 
  /**
   * Map from the synchronized blocks found in the flow unit to the 
   * set of locks acquired by each block.
   */
  private final Map<IRNode, Set<HeldLock>> syncBlocks = new HashMap<IRNode, Set<HeldLock>>();
  
  /**
   * The set of intrinsic locks that are held throughout the scope of the
   * method.  These are the locks known to be held because of
   * lock preconditions, the method being synchronized, the constructor
   * being single-threaded, or the flow unit being the class initializer.
   * These locks do not need to be tracked by the flow analysis because
   * they cannot be released during the lifetime of the flow unit.
   */
  private final Set<HeldLock> intrinsicAssumedLocks = new HashSet<HeldLock>();
 
  /**
   * Information for determining whether a constructor is proven single-threaded.
   * If the flow unit is not a constructor this is {@value null}.
   */
  private final SingleThreadedData singleThreadedData;
  
  
  
  /**
   * <code>mdecl</code> is always a top-level method: a ConstructorDeclaration,
   * MethodDeclaration, or ClassInitDeclaration.  It is never an
   * AnonClassExpression or InitDeclaration.
   */
  public LockExpressions(final IRNode mdecl, final LockUtils lu, final IBinder b,
      final BindingContextAnalysis bca) {
    enclosingMethodDecl = mdecl;
    final LockExpressionVisitor visitor =
      new LockExpressionVisitor(mdecl, lu, b, bca.getExpressionObjectsQuery(mdecl));
    visitor.doAccept(mdecl);
    singleThreadedData = visitor.getSingleThreadedData();
  }

  
  
  /**
   * Does the method use any JUC locks?
   */
  public boolean usesJUCLocks() {
    return !jucLockExprsToLockSets.isEmpty()
        || !jucRequiredLocks.isEmpty()
        || !jucSingleThreaded.isEmpty()
        || !jucClassInit.isEmpty();
  }
  
  /**
   * Does the method explicitly acquire or release any JUC locks?
   */
  public boolean invokesJUCLockMethods() {
    return !jucLockExprsToLockSets.isEmpty();
  }
  
  /**
   * Does the method use any intrinsic locks?
   */
  public boolean usesIntrinsicLocks() {
    return !syncBlocks.isEmpty()
        || !intrinsicAssumedLocks.isEmpty();
  }
  
  /**
   * Does the method have any sync blocks?
   */
  public boolean usesSynchronizedBlocks() {
    return !syncBlocks.isEmpty();
  }
  
  /**
   * Get the map of lock expressions to JUC locks.
   */
  public Map<IRNode, Set<HeldLock>> getJUCLockExprsToLockSets() {
    return Collections.unmodifiableMap(jucLockExprsToLockSets);
  }
  
  public Map<IRNode, Set<HeldLock>> getSyncBlocks() {
    return Collections.unmodifiableMap(syncBlocks);
  }
  
  /**
   * Get the JUC locks that appear in lock preconditions.
   */
  public Set<HeldLock> getJUCRequiredLocks() {
    return Collections.unmodifiableSet(jucRequiredLocks);
  }
  
  /**
   * Get the intrinsic locks that are held throughout the lifetime of the
   * flow unit.
   */
  public Set<HeldLock> getIntrinsicAssumedLocks() {
    return Collections.unmodifiableSet(intrinsicAssumedLocks);
  }
  
  /**
   * Get the single threaded data block for the flow unit.
   */
  public SingleThreadedData getSingleThreadedData() {
    return singleThreadedData;
  }
  
  /**
   * Get the JUC locks that are held because of being a singleThreaded constructor
   */
  public Set<HeldLock> getJUCSingleThreaded() {
    return Collections.unmodifiableSet(jucSingleThreaded);
  }
  
  /**
   * Get the JUC locks that are held because we are inside a class initializer
   */
  public Set<HeldLock> getJUCClassInit() {
    return Collections.unmodifiableSet(jucClassInit);
  }
  
  
  
  /* Taken from LocalVariableDeclarations.LocalDeclarationsVisitor.  Used
   * by LockExpressionVisitor below.
   */
  private enum WhichMembers {
    STATIC {
      @Override
      protected boolean acceptsModifier(final boolean isStatic) {
        return isStatic;
      }
    },
    INSTANCE {
      @Override
      protected boolean acceptsModifier(final boolean isStatic) {
        return !isStatic;
      }
    };
    
    protected abstract boolean acceptsModifier(boolean isStatic);
    
    public final boolean acceptsMember(final IRNode bodyDecl) {
      return acceptsModifier(JavaNode.getModifier(bodyDecl, JavaNode.STATIC));
    }
  }

  
  
  /**
   * A tree visitor that we run over a method/constructor body to find all the
   * occurrences of syntactically unique final lock expressions. 
   * @author Aaron Greenhouse
   */
  private final class LockExpressionVisitor extends VoidTreeWalkVisitor {
    private final LockUtils lockUtils;
    private final HeldLockFactory heldLockFactory;
    private final TEB teb;
    private BindingContextAnalysis.Query bcaQuery = null;
    // Set as a side-effect of visitConstructorDeclaration
    private SingleThreadedData singleThreadedData = null;    
    
    
    
    public LockExpressionVisitor(final IRNode mdecl, final LockUtils lu,
        final IBinder b, final BindingContextAnalysis.Query query) {
      super();
      lockUtils = lu;
      teb = new TEB(b, mdecl);
      heldLockFactory = new HeldLockFactory(teb);
      
      /* mdecl becomes the initial enclosingFlowUnit because the expectation is
       * that this visitor is then called with doVisit(mdecl).
       */
      bcaQuery = query;
    }
    
    public SingleThreadedData getSingleThreadedData() {
      return singleThreadedData;
    }
    
    
    
    // =========================================================================
    // == Helper method
    // == Taken from LocalVariableDeclarations.LocalVariableVisitor 
    // XXX Need to make a common superclass at some point
    // =========================================================================
    
    /**
     * Visit the immediate children of a classBody and handle the field
     * declarations and ClassInitializer blocks.
     * 
     * @param classBody
     *          A ClassBody IRNode.
     * @param isStatic
     *          <code>true</code> if we should process <code>static</code>
     *          fields and <code>static</code> initializer blocks only;
     *          <code>false</code> if we should process instance field
     *          declarations and instance initializer blocks only.
     */
    private void processClassBody(final IRNode classBody, final WhichMembers which) {
      for (final IRNode bodyDecl : ClassBody.getDeclIterator(classBody)) {
        final Operator op = JJNode.tree.getOperator(bodyDecl);
        if (FieldDeclaration.prototype.includes(op) ||
            ClassInitializer.prototype.includes(op)) {
          if (which.acceptsMember(bodyDecl)) {
            this.doAcceptForChildren(bodyDecl);
          }
        }       
      }
    }

    
    
    @Override
    public Void visitClassDeclaration(IRNode node) {
      /* STOP: we've encountered a class declaration.  We don't want to enter
       * the method declarations of nested class definitions.
       */
      return null;
    }

    @Override
    public Void visitInterfaceDeclaration(IRNode node) {
      /* STOP: we've encountered a class declaration.  We don't want to enter
       * the method declarations of nested class definitions.
       */
      return null;
    }

    @Override
    public Void visitEnumDeclaration(IRNode node) {
      /* STOP: we've encountered a class declaration.  We don't want to enter
       * the method declarations of nested class definitions.
       */
      return null;
    }

    @Override
    public Void visitAnonClassExpression(final IRNode node) {
      // Traverse into the arguments, but *not* the body
      doAccept(AnonClassExpression.getArgs(node));
      // Visit the field initializers and instance initializers
      
      
      final IRNode initMethod = JavaPromise.getInitMethodOrNull(node);
      final BindingContextAnalysis.Query oldQuery = bcaQuery;
      teb.newDeclaration(initMethod);
      bcaQuery = oldQuery.getSubAnalysisQuery(node);
      try {
        /* Need to visit the instance initializer blocks and instance field 
         * declarations.  We rely on the fact that anonymous classes cannot 
         * have static field members or static initializer blocks.  We ignore
         * method and constructor declarations.  I know this seems odd, but
         * see Bug 1662.  Technically the initializer blocks of the anonymous
         * class expression are executed as part of the enclosing method/constructor. 
         */
        processClassBody(AnonClassExpression.getBody(node), WhichMembers.INSTANCE);
      } finally {
        bcaQuery = oldQuery;
        teb.pop();
      }
      return null;
    }

    @Override
    public Void visitConstructorDeclaration(final IRNode cdecl) {
      final IRNode rcvr = JavaPromise.getReceiverNodeOrNull(cdecl);
      /* TODO: LockUtils method needs to make one pass through the 
       * locks and output to two sets: JUC and intrinsic. 
       */
      LockUtils.getLockPreconditions(HowToProcessLocks.JUC, cdecl, heldLockFactory, rcvr, jucRequiredLocks);
      LockUtils.getLockPreconditions(HowToProcessLocks.INTRINSIC, cdecl, heldLockFactory, rcvr, intrinsicAssumedLocks);
      singleThreadedData = lockUtils.isConstructorSingleThreaded(cdecl, rcvr);
      if (singleThreadedData.isSingleThreaded) {
        final IRNode classDecl = VisitUtil.getEnclosingType(cdecl);
        final IJavaDeclaredType clazz = JavaTypeFactory.getMyThisType(classDecl);
        /* TODO: LockUtils method needs to make one pass through the 
         * locks and output to two sets: JUC and intrinsic. 
         */
        lockUtils.getSingleThreadedLocks(HowToProcessLocks.JUC, cdecl, heldLockFactory, clazz, rcvr, jucSingleThreaded);
        lockUtils.getSingleThreadedLocks(HowToProcessLocks.INTRINSIC, cdecl, heldLockFactory, clazz, rcvr, intrinsicAssumedLocks);
      }
      
      // Analyze the body of the constructor
      doAcceptForChildren(cdecl);
      return null;
    }

    @Override
    public Void visitConstructorCall(final IRNode constructorCall) {
      // First process the constructor call and it's arguments
      doAcceptForChildren(constructorCall);
      
      final IRNode conObject = ConstructorCall.getObject(constructorCall);
      final Operator conObjectOp = JJNode.tree.getOperator(conObject);
      if (SuperExpression.prototype.includes(conObjectOp)) {
        // Visit the initializers.
        final BindingContextAnalysis.Query oldQuery = bcaQuery;
        bcaQuery = oldQuery.getSubAnalysisQuery(constructorCall);
        try {
          processClassBody(JJNode.tree.getParent(teb.enclosingFlowUnit), WhichMembers.INSTANCE);
        } finally {
          bcaQuery = oldQuery;
        }
      }
      return null;
    }

    @Override
    public Void visitMethodDeclaration(final IRNode mdecl) {
      final IRNode rcvr =
        TypeUtil.isStatic(mdecl) ? null : JavaPromise.getReceiverNodeOrNull(mdecl);

      /* TODO: LockUtils method needs to make one pass through the 
       * locks and output to two sets: JUC and intrinsic. 
       */
      LockUtils.getLockPreconditions(HowToProcessLocks.JUC, mdecl, heldLockFactory, rcvr, jucRequiredLocks);
      LockUtils.getLockPreconditions(HowToProcessLocks.INTRINSIC, mdecl, heldLockFactory, rcvr, intrinsicAssumedLocks);
      
      if (JavaNode.getModifier(mdecl, JavaNode.SYNCHRONIZED)) {
        final IRNode classDecl = VisitUtil.getEnclosingType(mdecl);
        final IJavaDeclaredType clazz = JavaTypeFactory.getMyThisType(classDecl);
        lockUtils.convertSynchronizedMethod(mdecl, heldLockFactory, rcvr, clazz, classDecl, intrinsicAssumedLocks);
      }
      
      doAcceptForChildren(mdecl);
      return null; 
    }
    
    @Override
    public Void visitClassInitDeclaration(final IRNode classInitDecl) {
      // Get the locks held due to class initialization assumptions
      final IRNode classDecl = JavaPromise.getPromisedFor(classInitDecl);
      final IJavaDeclaredType clazz = JavaTypeFactory.getMyThisType(classDecl);
      /* TODO: LockUtils method needs to make one pass through the 
       * locks and output to two sets: JUC and intrinsic. 
       */
      lockUtils.getClassInitLocks(HowToProcessLocks.JUC, classInitDecl, heldLockFactory, clazz, jucClassInit);
      lockUtils.getClassInitLocks(HowToProcessLocks.INTRINSIC, classInitDecl, heldLockFactory, clazz, intrinsicAssumedLocks);
      
      final IRNode classBody =
        AnonClassExpression.prototype.includes(classDecl) ?
            AnonClassExpression.getBody(classDecl) :
              TypeDeclaration.getBody(classDecl);
      processClassBody(classBody, WhichMembers.STATIC);
      return null;
    }
    
    @Override
    public Void visitInitDeclaration(final IRNode initDecl) {
      // XXX: Pretty sure we should never get here now. (2010-04-20)
      // XXX: I've had too many problems with this.  Throw an exception if we get here!
      throw new UnsupportedOperationException("Should not visit an InitDeclaration!");
    }
    
    @Override
    public Void visitMethodCall(final IRNode mcall) {
      processMethodCall(mcall);
      doAcceptForChildren(mcall);
      return null;
    }

    @Override
    public Void visitSynchronizedStatement(final IRNode syncBlock) {
      processSynchronized(syncBlock);
      doAcceptForChildren(syncBlock);
      return null;
    }
        
    /**
     * This method is shared by the body of {@link #visitMethodCall(IRNode)} in this
     * class and in the nested helper {@link InitializationVisitor#visitMethodCall(IRNode)}.
     * This makes it easier to ensure that both methods do the same thing.
     * @param mcall The method call node to process.
     */
    private void processMethodCall(final IRNode mcall) {
      if (lockUtils.isLockClassUsage(mcall)) {
        final MethodCall call = (MethodCall) JJNode.tree.getOperator(mcall);
        final IRNode lockExpr = call.get_Object(mcall);
        final Set<HeldLock> locks = 
          processLockExpression(HowToProcessLocks.JUC, lockExpr, null);
        if (locks != null) jucLockExprsToLockSets.put(lockExpr, locks);
      }
    }
    
    /**
     * This method is shared by the body of {@link #visitSynchronizedStatement(IRNode)} in this
     * class and in the nested helper {@link InitializationVisitor#visitSynchronizedStatement(IRNode)}.
     * This makes it easier to ensure that both methods do the same thing.
     * @param syncBlock The synchronized block to process
     */
    private void processSynchronized(final IRNode syncBlock) {
      final IRNode lockExpr = SynchronizedStatement.getLock(syncBlock);
      final Set<HeldLock> locks =
        processLockExpression(HowToProcessLocks.INTRINSIC, lockExpr, syncBlock);
      if (locks != null) syncBlocks.put(syncBlock, locks);
    }   
    
    private Set<HeldLock> processLockExpression(final HowToProcessLocks howTo,
        final IRNode lockExpr, final IRNode syncBlock) {
      if (lockUtils.getFinalExpressionChecker(bcaQuery, teb.enclosingFlowUnit, syncBlock).isFinal(lockExpr)) {
        // Get the locks for the lock expression
        final Set<HeldLock> lockSet = new HashSet<HeldLock>();
        lockUtils.convertLockExpr(
            howTo, lockExpr, heldLockFactory, LockExpressions.this.enclosingMethodDecl, syncBlock, lockSet);
        if (lockSet.isEmpty() && howTo == HowToProcessLocks.JUC) {
          lockSet.add(heldLockFactory.createBogusLock(lockExpr));
        }
        return lockSet;
      }
      return null;
    }    
  }
}
