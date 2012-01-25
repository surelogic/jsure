package edu.cmu.cs.fluid.sea.drops.promises;

import com.surelogic.aast.promise.*;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.SlotInfo;
import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.IDropFactory;
import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.util.*;

/**
 * Promise drop for "reads" and "writes" promises on methods
 * established by the uniqueness analysis.
 * 
 * @see edu.cmu.cs.fluid.java.analysis.EffectsAnalysis
 * @see edu.cmu.cs.fluid.java.bind.EffectsAnnotation
 */
public class MethodEffectsPromiseDrop extends PromiseDrop<EffectsSpecificationNode> {
  // One of the belows is stored as the default AST (in PromiseDrop)
  private ReadsNode reads;
  private WritesNode writes;
  
  public MethodEffectsPromiseDrop(EffectsSpecificationNode s) {
    super(s);
    setCategory(JavaGlobals.EFFECTS_CAT);
  }
  
  @Override
  public void setAST(EffectsSpecificationNode s) {
    if (getAST() == null) {
      super.setAST(s);
    }
    if (s instanceof ReadsNode) {
      ReadsNode r = (ReadsNode) s;
      if (checkAST(r, reads)) {
        return;
      }
      reads = r;
    } else {
      WritesNode w = (WritesNode) s;
      if (checkAST(w, writes)) {
        return;
      }
      writes = w;
    }
  }
  
  /**
   * @return true if bad
   */
  protected final <T extends EffectsSpecificationNode> 
  boolean checkAST(T s, T current) {
    if (checkASTs(s, current)) {
      return true;
    }
    IRNode n = getNode();
    if (n != null && !getNode().equals(s.getPromisedFor()))  {
      throw new IllegalArgumentException("different promisedFor nodes");
    }
    setNode(s.getPromisedFor());
    return false;
  }
  
  @Override
  public void addAST(EffectsSpecificationNode s) {
    setAST(s);
  }
  
  public ReadsNode getReads() {
    return reads;
  }
  
  public WritesNode getWrites() {
    return writes;
  }
  
  public Iterable<EffectSpecificationNode> getDeclaredEffects() {
    if (reads != null) {
      if (writes != null) {
        return new AppendIterator<EffectSpecificationNode>(reads.getEffectList().iterator(), 
                                  writes.getEffectList().iterator());
      } else {
        return getAST().getEffectList();
      }
    }
    if (writes != null) { // only writes
      return writes.getEffectList();
    }
    return new EmptyIterator<EffectSpecificationNode>();
  }
  
  @Override
  protected void computeBasedOnAST() {
    final IRNode declNode      = getNode();
    final String target        = JavaNames.genMethodConstructorName(declNode);
    EffectsSpecificationNode r = getReads();
    EffectsSpecificationNode w = getWrites();
    
     if (r == null) {
       if (w != null) {       
         setMessage(w.toString()+" on "+target);
       }
    } 
    else if (w == null) {
      setMessage(r.toString()+" on "+target);
    }
    else {
      setMessage(r+"; "+w+" on "+target);
    }
  }
  
  public static abstract class Factory<T extends EffectsSpecificationNode> implements IDropFactory<MethodEffectsPromiseDrop, T> {
    public final SlotInfo<MethodEffectsPromiseDrop> getSI() {
      throw new UnsupportedOperationException();
    }
    public final MethodEffectsPromiseDrop getDrop(IRNode n, T val) {
      throw new UnsupportedOperationException();
    }    
  }
}