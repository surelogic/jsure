package com.surelogic.aast;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.annotation.AnnotationSource;
import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNode;

public abstract class AASTRootNode extends AASTNode implements IAASTRootNode {
  private static final Logger LOG = SLLogger.getLogger("aast");
  
  private IRNode promisedFor;
  private AASTStatus status = AASTStatus.UNPROCESSED;
  private AnnotationSource srcType;
  
  protected AASTRootNode(int offset) {
    super(offset);    
  }  
  
  private static void checkArgument(Object oldVal, Object newVal) {
    if (oldVal != null || newVal == null) {
      throw new IllegalArgumentException();
    }
  }
   
  public final void setPromisedFor(IRNode n) {
    checkArgument(promisedFor, n);
    promisedFor = n;
  }
  
  public final void clearPromisedFor() {
	  promisedFor = null;
  }
  
  public final AnnotationSource getSrcType() {
    return srcType;
  } 
  
  public void setSrcType(AnnotationSource src) {
    checkArgument(srcType, src);
    srcType = src;
  }
  
  @Override
  public final IRNode getPromisedFor() {
    if (promisedFor == null && parent != null) {
      promisedFor = parent.getPromisedFor();
    }
    return promisedFor;
  } 
  
  public final AASTStatus getStatus() {
    return status;
  }
  
  public void markAsBound() {
    if (LOG.isLoggable(Level.FINER)) {
      LOG.finer("Bound OK for "+this.unparse(true));
    }
    if (status != AASTStatus.UNPROCESSED) {
      throw new IllegalArgumentException("Can only go from unprocessed to bound: "+status);
    }
    status = AASTStatus.BOUND;
  }

  public void markAsValid() {
//    System.out.println("Marked as VALID for "+this.unparse(false));
    if (status != AASTStatus.BOUND) {
      throw new IllegalArgumentException("Can only go from bound to valid: "+status);
    }    
    status = AASTStatus.VALID;
  }
  
  public void markAsUnbound() {
//    System.out.println("Marked as bogus: "+this.unparse(true));
    if (status == AASTStatus.VALID) {
      throw new IllegalArgumentException("Can't go from valid to bogus: "+status);
    }
    status = AASTStatus.UNBOUND;
  }
  
  public void markAsUnassociated() {
//    System.out.println("Marked as unassociated: "+this.unparse(true));
    if (status != AASTStatus.BOUND) {
      throw new IllegalArgumentException("Can only go from bound to unassociated: "+status);
    }
    status = AASTStatus.UNASSOCIATED;
  }
  
  @Override
  public abstract IAASTNode cloneTree();
  
  protected boolean isAbstract(int mods) {
    return JavaNode.isSet(mods, JavaNode.ABSTRACT);
  }
  
  protected boolean isStatic(int mods) {
    return JavaNode.isSet(mods, JavaNode.STATIC);
  }
  
  protected final boolean isSameClass(IAASTRootNode other) {
	  return other.getClass() == getClass();
  }
  
  public boolean implies(IAASTRootNode n) {
	  throw new UnsupportedOperationException();
  }
  
  public boolean isSameAs(IAASTRootNode n) {
	  throw new UnsupportedOperationException();
  }
}
