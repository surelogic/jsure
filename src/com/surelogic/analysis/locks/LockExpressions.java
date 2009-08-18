/*$Header: /cvs/fluid/fluid/src/com/surelogic/analysis/locks/LockExpressions.java,v 1.2 2009/02/17 14:01:32 aarong Exp $*/
package com.surelogic.analysis.locks;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.surelogic.aast.promise.LockSpecificationNode;
import com.surelogic.aast.promise.LockType;
import com.surelogic.analysis.locks.locks.HeldLock;
import com.surelogic.analysis.locks.locks.HeldLockFactory;
import com.surelogic.annotation.rules.LockRules;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.bind.JavaTypeFactory;
import edu.cmu.cs.fluid.java.operator.MethodCall;
import edu.cmu.cs.fluid.java.operator.VoidTreeWalkVisitor;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.drops.promises.LockModel;
import edu.cmu.cs.fluid.sea.drops.promises.RequiresLockPromiseDrop;
import edu.cmu.cs.fluid.sea.drops.promises.SingleThreadedPromiseDrop;

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

  /**
   * Map from final lock expressions used in synchronized statements to the
   * intrinsic locks they resolve to.
   */
  private final Map<IRNode, Set<HeldLock>> intrinsicLockExprsToLockSets =
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

  
  
  public LockExpressions(final IRNode mdecl, 
      final GlobalLockModel glm, final LockUtils lu, final HeldLockFactory hlf) {
    enclosingMethodDecl = mdecl;
    final LockExpressionVisitor visitor = new LockExpressionVisitor(glm, lu, hlf);
    visitor.doAccept(mdecl);
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
   * Does the method use any intrinsic locks?
   */
  public boolean usesIntrinsicLocks() {
    return !intrinsicLockExprsToLockSets.isEmpty();
//        || !jucRequiredLocks.isEmpty()
//        || !jucSingleThreaded.isEmpty()
//        || !jucClassInit.isEmpty();
  }
  
  /**
   * Get the map of lock expressions to JUC locks.
   */
  public Map<IRNode, Set<HeldLock>> getJUCLockExprsToLockSets() {
    return Collections.unmodifiableMap(jucLockExprsToLockSets);
  }
  
  /**
   * Get the map of lock expressions to intrinsic locks.
   */
  public Map<IRNode, Set<HeldLock>> getIntrinsicLockExprsToLockSets() {
    return Collections.unmodifiableMap(intrinsicLockExprsToLockSets);
  }
  
  /**
   * Get the JUC locks that appear in lock preconditions.
   */
  public Set<HeldLock> getJUCRequiredLocks() {
    return Collections.unmodifiableSet(jucRequiredLocks);
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
  final class LockExpressionVisitor extends VoidTreeWalkVisitor {
    
    private final LockUtils lockUtils;
    private final GlobalLockModel sysLockModel;
    private final HeldLockFactory heldLockFactory;
    
    
    
    public LockExpressionVisitor(final GlobalLockModel glm,
        final LockUtils lu, final HeldLockFactory hlf) {
      super();
      sysLockModel = glm;
      lockUtils = lu;
      heldLockFactory = hlf;
    }
    
    @Override
    public Void visitMethodDeclaration(final IRNode mdecl) {
      getJUCLockPreconditions(mdecl);
      doAcceptForChildren(mdecl);
      return null; 
    }
    
    @Override
    public Void visitClassInitDeclaration(final IRNode classInitDecl) {
      getClassInitLocks(classInitDecl);
      final InitializationVisitor iv = new InitializationVisitor(true);
      /* Must use accept for children because InitializationVisitor doesn't do anything
       * for ClassDeclaration nodes.  It's better this way anyhow because only care
       * about the children of the class declaration to begin with.
       */ 
      iv.doAcceptForChildren(JavaPromise.getPromisedFor(classInitDecl));
      return null;
    }
    
    @Override
    public Void visitInitDeclaration(final IRNode initDecl) {
      // XXX: Shouldn't get here?
      
      final InitializationVisitor iv = new InitializationVisitor(false);
      /* Must use accept for children because InitializationVisitor doesn't do anything
       * for ClassDeclaration nodes.  It's better this way anyhow because only care
       * about the children of the class declaration to begin with.
       */ 
      iv.doAcceptForChildren(JavaPromise.getPromisedFor(initDecl));
      return null;
    }
    
    private void getClassInitLocks(final IRNode classInitDecl) {
      /* Go through all the STATE locks in the class and pick out all the JUC
       * locks that protect static regions. 
       */
      final IRNode classDecl = VisitUtil.getEnclosingType(classInitDecl);
      final Set<RegionLockRecord> records =
        sysLockModel.getRegionLocksInClass(JavaTypeFactory.getMyThisType(classDecl));
      for (final RegionLockRecord lr : records) {
        if (lr.region.isStatic() && lr.lockDecl.isJUCLock()) {
          final HeldLock lock;
          if (lr.lockDecl.isReadWriteLock()) {
            lock = heldLockFactory.createJUCRWStaticLock(lr.lockDecl, classInitDecl, false, true);
          } else {
            lock = heldLockFactory.createJUCStaticLock(lr.lockDecl, classInitDecl, false);
          }
          jucClassInit.add(lock);
        }
      }
    }

    @Override
    public Void visitConstructorDeclaration(final IRNode cdecl) {
      getJUCLockPreconditions(cdecl);
      if (LockRules.isSingleThreaded(cdecl)) {
        getSingleThreadedLocks(cdecl);
      }
      // Analyze the initialization of the instance
      final InitializationVisitor helper = new InitializationVisitor(false);
      helper.doAcceptForChildren(JJNode.tree.getParent(cdecl));
      // Analyze the body of the constructor
      doAcceptForChildren(cdecl);
      return null;
    }
    
    private void getJUCLockPreconditions(final IRNode decl) {
      final IRNode rcvr = TypeUtil.isStatic(decl) ? null : JavaPromise.getReceiverNodeOrNull(decl);
      RequiresLockPromiseDrop drop = LockRules.getRequiresLock(decl);
      if (drop == null) {
        return;
      }
      for(final LockSpecificationNode requiredLock : drop.getAST().getLockList()) {
        // only process JUC locks.  Intrinsic locks are handled by the lock visitor
        final LockModel lockDecl = requiredLock.resolveBinding().getModel();
        if (lockDecl.isJUCLock(lockUtils)) {
          final HeldLock lock = lockUtils.convertLockNameToMethodContext(decl, requiredLock, true, drop, rcvr);
          /* Lock annotation sanity check guarantees that a lock can only appear
           * once in a given requiresLock annotation.
           */
          jucRequiredLocks.add(lock);
        }
      }
    }
    
    private void getSingleThreadedLocks(final IRNode cdecl) {
      /* Go through all the STATE locks in the class and pick out all the JUC
       * locks that protect instance regions. 
       */
      final SingleThreadedPromiseDrop drop = LockRules.getSingleThreadedDrop(cdecl);
      final IRNode receiverNode = JavaPromise.getReceiverNodeOrNull(cdecl);
      final IRNode classDecl = VisitUtil.getEnclosingType(cdecl);
      final Set<RegionLockRecord> records =
        sysLockModel.getRegionLocksInClass(JavaTypeFactory.getMyThisType(classDecl));
      for (final RegionLockRecord lr : records) {
        if (!lr.region.isStatic() && lr.lockDecl.isJUCLock()) {
          final HeldLock lock;
          if (lr.lockDecl.isReadWriteLock()) {
            lock = heldLockFactory.createJUCRWInstanceLock(receiverNode, lr.lockDecl, cdecl, drop, false, true);
          } else {
            lock = heldLockFactory.createJUCInstanceLock(receiverNode, lr.lockDecl, cdecl, drop, false);
          }
          jucSingleThreaded.add(lock);
        }
      }
    }
    
    @Override
    public Void visitMethodCall(final IRNode mcall) {
      processMethodCall(mcall);
      doAcceptForChildren(mcall);
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
        addJUCLockExpression(call.get_Object(mcall));
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
    public Void visitAnonClassExpression(IRNode node) {
      /* STOP: we've encountered a class declaration.  We don't want to enter
       * the method declarations of nested class definitions.
       */
      return null;
    }
    
    /**
     * Add the lock expression to the list if it is a final expression and not
     * already in the list.
     * @param lockExpr The lock expression to add.
     */
    private void addJUCLockExpression(final IRNode lockExpr) {
      if (lockUtils.isFinalExpression(lockExpr, null)) {
        // Get the locks for the lock expression
        final Set<HeldLock> lockSet = new HashSet<HeldLock>();
        lockUtils.convertJUCLockExpr(
            lockExpr, LockExpressions.this.enclosingMethodDecl, null, lockSet);
        if (lockSet.isEmpty()) {
          lockSet.add(heldLockFactory.createBogusLock(lockExpr));
        }
        LockExpressions.this.jucLockExprsToLockSets.put(lockExpr, lockSet);
      }
    }
    
    
    
    // =======================================================================
    
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
      public Void visitClassDeclaration(IRNode node) {
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
      public Void visitMethodCall(final IRNode mcall) {
        if (isInteresting) {
          LockExpressionVisitor.this.processMethodCall(mcall);
        }
        doAcceptForChildren(mcall);
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
