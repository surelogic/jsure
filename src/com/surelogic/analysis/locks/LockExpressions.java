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
import com.surelogic.annotation.rules.LockRules;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.bind.IJavaDeclaredType;
import edu.cmu.cs.fluid.java.bind.JavaTypeFactory;
import edu.cmu.cs.fluid.java.operator.MethodCall;
import edu.cmu.cs.fluid.java.operator.SynchronizedStatement;
import edu.cmu.cs.fluid.java.operator.VoidTreeWalkVisitor;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;

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

  /** Any intrinsic locks that are declared in a lock precondition */
  private final Set<HeldLock> intrinsicRequiredLocks = new HashSet<HeldLock>();

  /**
   * The JUC locks that apply because we are analyzing a singled-threaded 
   * constructor.
   */
  private final Set<HeldLock> jucSingleThreaded = new HashSet<HeldLock>();

  /**
   * The intrinsic locks that apply because we are analyzing a singled-threaded 
   * constructor.
   */
  private final Set<HeldLock> intrinsicSingleThreaded = new HashSet<HeldLock>();

  /**
   * The JUC locks that apply because we are analyzing the class initializer.
   */
  private final Set<HeldLock> jucClassInit = new HashSet<HeldLock>();

  /**
   * The intrinsic locks that apply because we are analyzing the class initializer.
   */
  private final Set<HeldLock> intrinsicClassInit = new HashSet<HeldLock>();

  
  
  public LockExpressions(
      final IRNode mdecl, final LockUtils lu, final HeldLockFactory hlf) {
    enclosingMethodDecl = mdecl;
    final LockExpressionVisitor visitor = new LockExpressionVisitor(lu, hlf);
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
    return !intrinsicLockExprsToLockSets.isEmpty()
        || !intrinsicRequiredLocks.isEmpty()
        || !intrinsicSingleThreaded.isEmpty()
        || !intrinsicClassInit.isEmpty();
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
   * Get the intrinsic locks that appear in lock preconditions.
   */
  public Set<HeldLock> getIntrinsicRequiredLocks() {
    return Collections.unmodifiableSet(intrinsicRequiredLocks);
  }
  
  /**
   * Get the JUC locks that are held because of being a singleThreaded constructor
   */
  public Set<HeldLock> getJUCSingleThreaded() {
    return Collections.unmodifiableSet(jucSingleThreaded);
  }
  
  /**
   * Get the intrinsic locks that are held because of being a singleThreaded constructor
   */
  public Set<HeldLock> getIntrinsicSingleThreaded() {
    return Collections.unmodifiableSet(intrinsicSingleThreaded);
  }
  
  /**
   * Get the JUC locks that are held because we are inside a class initializer
   */
  public Set<HeldLock> getJUCClassInit() {
    return Collections.unmodifiableSet(jucClassInit);
  }
  
  /**
   * Get the intrinsic locks that are held because we are inside a class initializer
   */
  public Set<HeldLock> getIntrinsicClassInit() {
    return Collections.unmodifiableSet(intrinsicClassInit);
  }

  
  
  /**
   * A tree visitor that we run over a method/constructor body to find all the
   * occurrences of syntactically unique final lock expressions. 
   * 
   * @author Aaron Greenhouse
   */
  final class LockExpressionVisitor extends VoidTreeWalkVisitor {
    
    private final LockUtils lockUtils;
    private final HeldLockFactory heldLockFactory;
    
    
    
    public LockExpressionVisitor(final LockUtils lu, final HeldLockFactory hlf) {
      super();
      lockUtils = lu;
      heldLockFactory = hlf;
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
      getLockPreconditions(cdecl);
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

    @Override
    public Void visitMethodDeclaration(final IRNode mdecl) {
      getLockPreconditions(mdecl);
      doAcceptForChildren(mdecl);
      return null; 
    }
    
    @Override
    public Void visitClassInitDeclaration(final IRNode classInitDecl) {
      getClassInitLocks(classInitDecl);
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
      // XXX: Shouldn't get here?
      
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
    
    private void getClassInitLocks(final IRNode classInitDecl) {
      final IRNode classDecl = VisitUtil.getEnclosingType(classInitDecl);
      final IJavaDeclaredType clazz = JavaTypeFactory.getMyThisType(classDecl);
      /* TODO: LockUtils method needs to make one pass through the 
       * locks and output to two sets: JUC and intrinsic. 
       */
      lockUtils.getClassInitLocks(HowToProcessLocks.JUC, classInitDecl, clazz, jucClassInit);
      lockUtils.getClassInitLocks(HowToProcessLocks.INTRINSIC, classInitDecl, clazz, intrinsicClassInit);
    }

    private void getLockPreconditions(final IRNode decl) {
      final IRNode rcvr =
        TypeUtil.isStatic(decl) ? null : JavaPromise.getReceiverNodeOrNull(decl);
      /* TODO: LockUtils method needs to make one pass through the 
       * locks and output to two sets: JUC and intrinsic. 
       */
      lockUtils.getLockPreconditions(HowToProcessLocks.JUC, decl, rcvr, jucRequiredLocks);
      lockUtils.getLockPreconditions(HowToProcessLocks.INTRINSIC, decl, rcvr, intrinsicRequiredLocks);
    }
    
    private void getSingleThreadedLocks(final IRNode cdecl) {
      final IRNode receiverNode = JavaPromise.getReceiverNodeOrNull(cdecl);
      final IRNode classDecl = VisitUtil.getEnclosingType(cdecl);
      final IJavaDeclaredType clazz = JavaTypeFactory.getMyThisType(classDecl);
      /* TODO: LockUtils method needs to make one pass through the 
       * locks and output to two sets: JUC and intrinsic. 
       */
      lockUtils.getSingleThreadedLocks(HowToProcessLocks.JUC, cdecl, clazz, receiverNode, jucSingleThreaded);
      lockUtils.getSingleThreadedLocks(HowToProcessLocks.INTRINSIC, cdecl, clazz, receiverNode, intrinsicSingleThreaded);
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
        addLockExpression(HowToProcessLocks.JUC, call.get_Object(mcall), null, jucLockExprsToLockSets);
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
      addLockExpression(HowToProcessLocks.INTRINSIC, lockExpr, syncBlock, intrinsicLockExprsToLockSets);
    }

    private void addLockExpression(final HowToProcessLocks howTo,
        final IRNode lockExpr, final IRNode syncBlock, final Map<IRNode, Set<HeldLock>> map) {
      if (lockUtils.isFinalExpression(lockExpr, syncBlock)) {
        // Get the locks for the lock expression
        final Set<HeldLock> lockSet = new HashSet<HeldLock>();
        lockUtils.convertLockExpr(
            howTo, lockExpr, LockExpressions.this.enclosingMethodDecl, syncBlock, lockSet);
        if (lockSet.isEmpty() && howTo == HowToProcessLocks.JUC) {
          lockSet.add(heldLockFactory.createBogusLock(lockExpr));
        }
        map.put(lockExpr, lockSet);
      }
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
      public Void visitClassDeclaration(final IRNode node) {
        /* STOP: we've encountered a class declaration.  We don't want to enter
         * the method declarations of nested class definitions.
         */
        return null;
      }

      @Override
      public Void visitInterfaceDeclaration(final IRNode node) {
        /* STOP: we've encountered a class declaration.  We don't want to enter
         * the method declarations of nested class definitions.
         */
        return null;
      }

      @Override
      public Void visitEnumDeclaration(final IRNode node) {
        /* STOP: we've encountered a class declaration.  We don't want to enter
         * the method declarations of nested class definitions.
         */
        return null;
      }

      @Override
      public Void visitAnonClassExpression(final IRNode node) {
        /* STOP: we've encountered a class declaration.  We don't want to enter
         * the method declarations of nested class definitions.
         */
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
