/*
 * Created on Dec 9, 2004
 *
 */
package edu.cmu.cs.fluid.sea.drops.promises;

import java.util.logging.Level;

import com.surelogic.aast.promise.*;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.sea.*;
import edu.cmu.cs.fluid.sea.drops.CUDrop;

/**
 * getNode() should return the associated promise declaration
 * 
 * @author Edwin
 *
 */
public abstract class ModelDrop<D extends PromiseDeclarationNode> extends PromiseDrop<D> {
  public ModelDrop(D d) {
    super(d);
  }
  
  public ModelDrop() {
    super();
  }
  
  protected boolean okAsNode(IRNode n) {
    return true; // TODO PromiseDeclaration.prototype.includes(n);
  }
  
  @Override
  protected void deponentInvalidAction(Drop invalidDeponent) {    
    // invalidate();
    if (invalidDeponent instanceof CUDrop) {
      //System.out.println("Invalidating "+getMessage());
      CUDrop cud = (CUDrop) invalidDeponent;
      IRNode cu  = VisitUtil.getEnclosingCompilationUnit(getNode());
      if (cu != null && !cud.cu.equals(cu)) {
        throw new Error("unexpected dependence on CUDrop: "+DebugUnparser.toString(cud.cu));
      }
      clearNode();
      clearAST();      
      //System.out.println("Clearing "+this.getMessage()+" due to invalidated "+invalidDeponent.getMessage());
    } else {
      /*
      for(Drop d : getDeponents()) {
    	  System.out.println(d.getClass().getSimpleName()+": "+d.getMessage());
      }
      */
      if (LOG.isLoggable(Level.FINE)) {
    	  LOG.fine("Unexpected invalidate on "+getMessage()+" from "+invalidDeponent.getMessage());
      }
    }
  }
  
  protected static boolean modelDefinedInCode(DropPredicate definingDropPred, ModelDrop drop) {
    return drop.isValid() &&
           (drop.getNode() != null || drop.hasMatchingDependents(definingDropPred));
  }
}
