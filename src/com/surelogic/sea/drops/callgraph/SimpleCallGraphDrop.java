/*
 * Created on Nov 6, 2004
 *
 */
package com.surelogic.sea.drops.callgraph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import com.surelogic.annotation.rules.ColorRules;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNode;


import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.Drop;
import edu.cmu.cs.fluid.sea.PhantomDrop;


/**
 * @author dfsuther
 *
 * Holds the caller and callee information for a single method or constructor.  There
 * should be no more than one of these drops for any specific IRNode.
 */
public class SimpleCallGraphDrop extends PhantomDrop {
  private final int initSetSize = 2;
  private final Collection<IRNode> callers = new HashSet<IRNode>(initSetSize);
  private final Collection<IRNode> callees = new HashSet<IRNode>(initSetSize);
  private boolean colorsNeedBodyTraversal = false;
  private boolean foundABody = false;
  
  private boolean potentiallyCallable = false;
  
  private static Collection<SimpleCallGraphDrop> allCGDrops = new HashSet<SimpleCallGraphDrop>();
  
//  public Integer moduleNum = null;
//  public boolean partOfAPI = false;
  private IRNode theBody = null;
  
  public int numCallSitesSeen = 0;
  public int numOverridingMethods = 0;
  
  private IRNode outerTypeOrCU = null;
  
  //private int numCGDrops = 0;
  private static SimpleCallGraphDrop cgDropProto = new SimpleCallGraphDrop();
  
  
  private SimpleCallGraphDrop() {
  }
  
  private SimpleCallGraphDrop(IRNode node) {
    ColorRules.setCGDrop(node, this);
    setNodeAndCompilationUnitDependency(node);
    setMessage(/*"SimpleCallGraphDrop for " + */JJNode.getInfo(node));
    synchronized (SimpleCallGraphDrop.class) {
      allCGDrops.add(this);
    }
    outerTypeOrCU = VisitUtil.computeOutermostEnclosingTypeOrCU(node);
  }
  
  public static SimpleCallGraphDrop getCGDropFor(IRNode node) {
    SimpleCallGraphDrop res = ColorRules.getCGDrop(node);
    
    if (res == null) {
      res = new SimpleCallGraphDrop(node);
    }
    
    return res;
  }
  
  

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.sea.Drop#deponentInvalidAction()
   */
  @Override
  protected void deponentInvalidAction(Drop invalidDeponent) {
    super.deponentInvalidAction(invalidDeponent);
    synchronized (SimpleCallGraphDrop.class) {
      allCGDrops.remove(this);
    }
    final IRNode myNode = getNode();
    // remove myNode from the caller sets of all my callees.
    Iterator<IRNode> calleeIter = callees.iterator();
    while (calleeIter.hasNext()) {
      IRNode callee = calleeIter.next();
      SimpleCallGraphDrop calleeCGD = ColorRules.getCGDrop(callee);
      if ((calleeCGD == null) || (calleeCGD == this)) continue;
      calleeCGD.callers.remove(myNode);
    }
    // remove myNode from the callee sets of all my callers.
    Iterator<IRNode> callerIter = callers.iterator();
    while (callerIter.hasNext()) {
      IRNode caller = callerIter.next();
      SimpleCallGraphDrop callerCGD = ColorRules.getCGDrop(caller);
      if ((callerCGD == null) || (callerCGD == this)) continue;
      callerCGD.callees.remove(myNode);
    }
    // clear my own callees and callers.  This will take care of any recursive calls
    // that I skipped above (so I wouldn't be altering the Collection while iterating
    // over it).
    callees.clear();
    callers.clear();
    
    // clear this call graph drop out of any module it appears in.
//    ModuleSupport.getINSTANCE().nukingACGD(this);
  }
  /**
   * @return Returns the callees.
   */
  public Collection<IRNode> getCallees() {
    return callees;
  }
  /**
   * @return Returns the callers.
   */
  public Collection<IRNode> getCallers() {
    return callers;
  }
 
  /**
   * @return Returns the allCGDrops.
   */
  public static synchronized SimpleCallGraphDrop[] getAllCGDrops() { 
    SimpleCallGraphDrop[] res = new SimpleCallGraphDrop[allCGDrops.size()];
    res = allCGDrops.toArray(res);
    return res;
  }
  /**
   * @return Returns the colorsNeedBodyTraversal.
   */
  public boolean colorsNeedBodyTraversal() {
    return colorsNeedBodyTraversal;
  }
  /**
   * @param needBodyTraversal The colorsNeedBodyTraversal to set.
   */
  public void setColorsNeedBodyTraversal(boolean needBodyTraversal) {
    this.colorsNeedBodyTraversal = needBodyTraversal;
  }
  /**
   * @return Returns the bodyFound.
   */
  public boolean foundABody() {
    return foundABody;
  }
  /**
   * @param bodyFound The bodyFound to set.
   */
  public void setFoundABody(boolean bodyFound) {
    this.foundABody = bodyFound;
  }
  /**
   * @return Returns the theBody.
   */
  public IRNode getTheBody() {
    return theBody;
  }
  
  public boolean hasCallers() {
    return callers != null && !callers.isEmpty();
  }
  
  public boolean makesCalls() {
    return callees != null && !callees.isEmpty();
  }
  
  /**
   * @param theBody The theBody to set.
   */
  public void setTheBody(IRNode theBody) {
    this.theBody = theBody;
    if (theBody != null) {
      foundABody = true;
    }
  }

  public static class CGStats {
    public int numDrops = 0;
    public int numAPI = 0;
    public int numAPInoCallers = 0;
    public int numAPInoCallees = 0;
    public int numWithCallers = 0;
    public int numWithCallees = 0;
  
  }
  
  private CGStats newCGStats() {
    return new CGStats();
  }
  public static CGStats getStats() {
    CGStats res = cgDropProto.newCGStats();
    Collection<SimpleCallGraphDrop> deleteUs = new ArrayList<SimpleCallGraphDrop>();
    for (Iterator<SimpleCallGraphDrop> cgIter = allCGDrops.iterator(); cgIter.hasNext();) {
      SimpleCallGraphDrop aCGD = cgIter.next();
      final IRNode node = aCGD.getNode();
      if (aCGD.isValid()) {
        //TODO: uncomment next block to get stats back!
//        final ModuleModel myModule = ModuleModel.getModuleDrop(node);
//        final boolean isAPI = myModule.isAPI(node);
//        res.numDrops += 1;
//        if (isAPI) {
//          res.numAPI += 1;
//        }
//        if (aCGD.hasCallers()) {
//          res.numWithCallers += 1;
//        } else {
//          // not called
//          if (isAPI) {
//            res.numAPInoCallers += 1;
//          }
//        }
//        if (aCGD.makesCalls()) {
//          res.numWithCallees += 1;
//        } else {
//          if (isAPI) {
//            res.numAPInoCallees += 1;
//          }
//        }
      } else {
        deleteUs.add(aCGD);
      }
    }
    
    allCGDrops.removeAll(deleteUs);
    
    return res;
  }

  
  /**
   * @return Returns the outerTypeOrCU.
   */
  public IRNode getOuterTypeOrCU() {
    return outerTypeOrCU;
  }

  public boolean isPotentiallyCallable() {
    return potentiallyCallable;
  }

  public void setPotentiallyCallable(boolean potentiallyCallable) {
    this.potentiallyCallable = potentiallyCallable;
  }

  
}
