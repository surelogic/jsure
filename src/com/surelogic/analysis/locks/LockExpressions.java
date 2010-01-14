/*$Header: /cvs/fluid/fluid/src/com/surelogic/analysis/locks/LockExpressions.java,v 1.2 2009/02/17 14:01:32 aarong Exp $*/
package com.surelogic.analysis.locks;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.surelogic.analysis.locks.LockUtils.HowToProcessLocks;
import com.surelogic.analysis.locks.locks.HeldLock;
import com.surelogic.analysis.locks.locks.HeldLockFactory;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.bind.IJavaDeclaredType;
import edu.cmu.cs.fluid.java.bind.JavaTypeFactory;
import edu.cmu.cs.fluid.java.operator.AnonClassExpression;
import edu.cmu.cs.fluid.java.operator.ConstructorCall;
import edu.cmu.cs.fluid.java.operator.MethodCall;
import edu.cmu.cs.fluid.java.operator.SuperExpression;
import edu.cmu.cs.fluid.java.operator.SynchronizedStatement;
import edu.cmu.cs.fluid.java.operator.VoidTreeWalkVisitor;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.drops.effects.RegionEffectsPromiseDrop;
import edu.cmu.cs.fluid.sea.drops.promises.BorrowedPromiseDrop;
import edu.cmu.cs.fluid.sea.drops.promises.StartsPromiseDrop;
import edu.cmu.cs.fluid.sea.drops.promises.UniquePromiseDrop;
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
 * @author aarong
 */
final class LockExpressions {
  final public static class SingleThreadedData {
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
  
  
  
  public LockExpressions(
      final IRNode mdecl, final LockUtils lu, final HeldLockFactory hlf) {
    enclosingMethodDecl = mdecl;
    final LockExpressionVisitor visitor = new LockExpressionVisitor(lu, hlf);
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
  
  /**
   * A tree visitor that we run over a method/constructor body to find all the
   * occurrences of syntactically unique final lock expressions. 
   * 
   * @author Aaron Greenhouse
   */
  private final class LockExpressionVisitor extends VoidTreeWalkVisitor {
    
    private final LockUtils lockUtils;
    private final HeldLockFactory heldLockFactory;
    // Set as a side-effect of visitConstructorDeclaration
    private SingleThreadedData singleThreadedData = null;
    
    private IRNode enclosingFlowUnit = null;
    
    public LockExpressionVisitor(final LockUtils lu, final HeldLockFactory hlf) {
      super();
      lockUtils = lu;
      heldLockFactory = hlf;
    }
    
    public SingleThreadedData getSingleThreadedData() {
      return singleThreadedData;
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
    public Void visitAnonClassExpression(IRNode node) {
      /* STOP: we've encountered a class declaration.  We don't want to enter
       * the method declarations of nested class definitions.
       */
      return null;
    }

    @Override
    public Void visitConstructorDeclaration(final IRNode cdecl) {
      final IRNode rcvr = JavaPromise.getReceiverNodeOrNull(cdecl);
      /* TODO: LockUtils method needs to make one pass through the 
       * locks and output to two sets: JUC and intrinsic. 
       */
      lockUtils.getLockPreconditions(HowToProcessLocks.JUC, cdecl, rcvr, jucRequiredLocks);
      lockUtils.getLockPreconditions(HowToProcessLocks.INTRINSIC, cdecl, rcvr, intrinsicAssumedLocks);
      singleThreadedData = lockUtils.isConstructorSingleThreaded(cdecl, rcvr);
      if (singleThreadedData.isSingleThreaded) {
        final IRNode classDecl = VisitUtil.getEnclosingType(cdecl);
        final IJavaDeclaredType clazz = JavaTypeFactory.getMyThisType(classDecl);
        /* TODO: LockUtils method needs to make one pass through the 
         * locks and output to two sets: JUC and intrinsic. 
         */
        lockUtils.getSingleThreadedLocks(HowToProcessLocks.JUC, cdecl, clazz, rcvr, jucSingleThreaded);
        lockUtils.getSingleThreadedLocks(HowToProcessLocks.INTRINSIC, cdecl, clazz, rcvr, intrinsicAssumedLocks);
      }
      
      // Analyze the initialization of the instance
      enclosingFlowUnit = cdecl;
      try {
        // we now analyze the initializers from visitConstructorCall
//        final InitializationVisitor helper = new InitializationVisitor(false);
//        helper.doAcceptForChildren(JJNode.tree.getParent(cdecl));
        // Analyze the body of the constructor
        doAcceptForChildren(cdecl);
      } finally {
        enclosingFlowUnit = null;
      }
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
        final InitializationVisitor helper = new InitializationVisitor(false);
        helper.doAcceptForChildren(JJNode.tree.getParent(enclosingFlowUnit));
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
      lockUtils.getLockPreconditions(HowToProcessLocks.JUC, mdecl, rcvr, jucRequiredLocks);
      lockUtils.getLockPreconditions(HowToProcessLocks.INTRINSIC, mdecl, rcvr, intrinsicAssumedLocks);
      
      if (JavaNode.getModifier(mdecl, JavaNode.SYNCHRONIZED)) {
        final IRNode classDecl = VisitUtil.getEnclosingType(mdecl);
        final IJavaDeclaredType clazz = JavaTypeFactory.getMyThisType(classDecl);
        lockUtils.convertSynchronizedMethod(mdecl, rcvr, clazz, classDecl, intrinsicAssumedLocks);
      }
      
      enclosingFlowUnit = mdecl;
      try {
        doAcceptForChildren(mdecl);
      } finally {
        enclosingFlowUnit = null;
      }
      return null; 
    }
    
    @Override
    public Void visitClassInitDeclaration(final IRNode classInitDecl) {
      // Get the locks held due to class initialization assumptions
      final IRNode classDecl = VisitUtil.getEnclosingType(classInitDecl);
      final IJavaDeclaredType clazz = JavaTypeFactory.getMyThisType(classDecl);
      /* TODO: LockUtils method needs to make one pass through the 
       * locks and output to two sets: JUC and intrinsic. 
       */
      lockUtils.getClassInitLocks(HowToProcessLocks.JUC, classInitDecl, clazz, jucClassInit);
      lockUtils.getClassInitLocks(HowToProcessLocks.INTRINSIC, classInitDecl, clazz, intrinsicAssumedLocks);
      
      final InitializationVisitor iv = new InitializationVisitor(true);
      /* Must use accept for children because InitializationVisitor doesn't do anything
       * for ClassDeclaration nodes.  It's better this way anyhow because we only care
       * about the children of the class declaration to begin with.
       */ 
      iv.doAcceptForChildren(JavaPromise.getPromisedFor(classInitDecl));
      return null;
    }
    
    @Override
    public Void visitInitDeclaration(final IRNode initDecl) {
      /* We get here when the original expression that spawn the request
       * to get the LockExpressiosn object is inside the instance initializer
       * block of an AnonClassExpression.
       * 
       * We don't get here for instance initializers in regular class
       * declarations because those are taken care of in visitConstructorDeclaration
       * by creating an InitializationVisitor there, were they are ultimately
       * visited by InitializationVisito.visitClassInitiazer(). 
       */
      
      final InitializationVisitor iv = new InitializationVisitor(false);
      /* Must use accept for children because InitializationVisitor doesn't do anything
       * for ClassDeclaration nodes.  It's better this way anyhow because only care
       * about the children of the class declaration to begin with.
       */ 
      iv.doAcceptForChildren(JavaPromise.getPromisedFor(initDecl));
      return null;
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
      if (lockUtils.getFinalExpressionChecker(enclosingFlowUnit, syncBlock).isFinal(lockExpr)) {
        // Get the locks for the lock expression
        final Set<HeldLock> lockSet = new HashSet<HeldLock>();
        lockUtils.convertLockExpr(
            howTo, lockExpr, LockExpressions.this.enclosingMethodDecl, syncBlock, lockSet);
        if (lockSet.isEmpty() && howTo == HowToProcessLocks.JUC) {
          lockSet.add(heldLockFactory.createBogusLock(lockExpr));
        }
        return lockSet;
      }
      return null;
    }    

    
    // =======================================================================
    
    /**
     * Used to visit the class in the context of a ClassInitDeclaration
     * or a InitDeclaration.  Basically we want to look at the field initializers
     * and the static or instance initializer blocks.  We don't want to go
     * inside of any other members of the class, especially not method or 
     * constructor declarations.
     */
    private final class InitializationVisitor extends VoidTreeWalkVisitor {
      /** 
       * Are we matching static initializers?  if <code>false</code> we match
       * instance fields and initializers.
       */
      private final boolean isStatic; 
      
      /**
       * Flag that indicates whether we are inside a field declaration
       * or a initializer block that is interesting.
       */
      private boolean isInteresting = false;
      
      
      
      public InitializationVisitor(final boolean matchStatic) {
        isStatic = matchStatic;
      }
      
      
      
      private boolean triggersInterest(final IRNode node) {
        return (TypeUtil.isStatic(node) == isStatic);
      }
      
      
      
      @Override
      public Void visitTypeDeclaration(final IRNode node) {
        /* STOP: we've encountered a type declaration.  We don't want to enter
         * the method declarations of nested class definitions.
         */
        return null;
      }
      
      @Override 
      public Void visitAnonClassExpression(final IRNode expr) {
        // Traverse into the arguments, but *not* the body
        doAccept(AnonClassExpression.getArgs(expr));
        return null;
      }

      @Override
      public Void visitMethodDeclaration(final IRNode node) {
        /* STOP: we've encountered a method declaration. 
         */
        return null;
      }

      @Override
      public Void visitConstructorDeclaration(final IRNode node) {
        /* STOP: we've encountered a method declaration. 
         */
        return null;
      }

      
      
      @Override
      public Void visitMethodCall(final IRNode mcall) {
        if (isInteresting) {
          LockExpressionVisitor.this.processMethodCall(mcall);
        }
        doAcceptForChildren(mcall);
        return null;
      }

      @Override
      public Void visitSynchronizedStatement(final IRNode syncBlock) {
        processSynchronized(syncBlock);
        doAcceptForChildren(syncBlock);
        return null;
      }

      
      
      @Override
      public Void visitClassInitializer(final IRNode expr) {
        if (triggersInterest(expr)) {
          try {
            isInteresting = true;
            doAcceptForChildren(expr);
          } finally {
            isInteresting = false;
          }
        }
        return null;
      }
      
      @Override
      public Void visitFieldDeclaration(final IRNode fd) {
        if (triggersInterest(fd)) {
          try {
            isInteresting = true;
            doAcceptForChildren(fd);
          } finally {
            isInteresting = false;
          }
        }
        return null;
      }
    }
  }
}
