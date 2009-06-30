package edu.cmu.cs.fluid.sea.drops.promises;

import java.util.HashMap;
import java.util.Map;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.sea.PromiseDrop;

/**
 * Promise drop that represents a point in the code where a lock is
 * acquired.  The associated IRNode is either a SynchronizedStatement,
 * a MethodDeclaration of a synchronized method, a 
 * ConstructorDeclaration of a <code>@singleThreaded</code> 
 * constructor, or a <code>@requiresLock</code> annotation. 
 */
public final class LockAcquisitionPoint extends PromiseDrop {
  private static final Map<IRNode,LockAcquisitionPoint> syncBlockToDrop = 
    new HashMap<IRNode,LockAcquisitionPoint>();
  private static final Map<IRNode,LockAcquisitionPoint> syncMethodToDrop =
    new HashMap<IRNode,LockAcquisitionPoint>();
  private static final Map<IRNode,LockAcquisitionPoint> singleThreadedConstructorToDrop =
    new HashMap<IRNode,LockAcquisitionPoint>();
  private static final Map<IRNode,LockAcquisitionPoint> requiresLockToDrop = 
    new HashMap<IRNode,LockAcquisitionPoint>();
  
  /*
  private LockAcquisitionPoint(final IRNode point) {
    this.setMessage(DebugUnparser.toString(point));
    this.setNodeAndCompilationUnitDependency(point);
    this.setFromSrc(false);
    // Don't set category?
  }
  */
  
  private LockAcquisitionPoint(final String msg, final IRNode point) {
    this.setMessage(msg);
    this.setNodeAndCompilationUnitDependency(point);
    this.setFromSrc(false);
    this.dependUponCompilationUnitOf(point);
    // Don't set category?
  }
  
  /**
   * @param point A SynchronizeStatement node
   */
  public static synchronized LockAcquisitionPoint getSyncBlockDrop(final IRNode point) {
    LockAcquisitionPoint drop = syncBlockToDrop.get(point);
    if (drop == null) {
      drop = new LockAcquisitionPoint("Synchronized statement " + DebugUnparser.toString(point), point);
      /* Should be caching these, because we really only want one drop per
       * acquisition point, but I don't have a good way right now of only
       * generating the supporting information once, which currently is much
       * easier to due where the drop is used.
       * 
       * Right now the caching and set up of the drop has been abstracted 
       * into factory methods in class LockAnalysis, where the necessary
       * infomration to fully generate the drop can be found.
       */
//      syncBlockToDrop.put(point, drop);
    }
    return drop;
  }
  
  /**
   * @param point A MethodDeclaration node of a synchronized method
   */
  public static synchronized LockAcquisitionPoint getSyncMethodDrop(final IRNode point) {
    LockAcquisitionPoint drop = syncMethodToDrop.get(point);
    if (drop == null) {
      drop = new LockAcquisitionPoint("Synchronized method " + JavaNames.genMethodConstructorName(point), point);
      /* Should be caching these, because we really only want one drop per
       * acquisition point, but I don't have a good way right now of only
       * generating the supporting information once, which currently is much
       * easier to due where the drop is used.
       * 
       * Right now the caching and set up of the drop has been abstracted 
       * into factory methods in class LockAnalysis, where the necessary
       * infomration to fully generate the drop can be found.
       */
//      syncMethodToDrop.put(point, drop);
    }
    return drop;
  }
  
  /**
   * @param point A ConstructorDeclaration node of a singleThreaded constructor
   */
  public static synchronized LockAcquisitionPoint getSingleThreadedConstructorDrop(final IRNode point) {
    LockAcquisitionPoint drop = singleThreadedConstructorToDrop.get(point);
    if (drop == null) {
      drop = new LockAcquisitionPoint("Single-threaded constructor " + JavaNames.genMethodConstructorName(point), point);
      /* Should be caching these, because we really only want one drop per
       * acquisition point, but I don't have a good way right now of only
       * generating the supporting information once, which currently is much
       * easier to due where the drop is used.
       * 
       * Right now the caching and set up of the drop has been abstracted 
       * into factory methods in class LockAnalysis, where the necessary
       * infomration to fully generate the drop can be found.
       */
//    singleThreadedConstructorToDrop.put(point, drop);
    }
    return drop;
  }
  
  /**
   * @param point A MethodDeclaration node of a method with a requires lock annotation
   */
  public static synchronized LockAcquisitionPoint getRequiresLockDrop(final IRNode point) {
    LockAcquisitionPoint drop = requiresLockToDrop.get(point);
    if (drop == null) {
      drop = new LockAcquisitionPoint("Lock preconditions ", point);
      /* Should be caching these, because we really only want one drop per
       * acquisition point, but I don't have a good way right now of only
       * generating the supporting information once, which currently is much
       * easier to due where the drop is used.
       * 
       * Right now the caching and set up of the drop has been abstracted 
       * into factory methods in class LockAnalysis, where the necessary
       * infomration to fully generate the drop can be found.
       */
//    requiresLockToDrop.put(point, drop);
    }
    return drop;
  }
}
