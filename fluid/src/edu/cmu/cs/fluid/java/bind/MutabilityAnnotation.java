/*
 * Created on Oct 30, 2003
 *
 */
package edu.cmu.cs.fluid.java.bind;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.SlotInfo;
import edu.cmu.cs.fluid.promise.*;
import edu.cmu.cs.fluid.promise.parse.BooleanTagRule;
import edu.cmu.cs.fluid.tree.Operator;

/**
 * @author chance
 *  
 */
@Deprecated
public final class MutabilityAnnotation extends AbstractPromiseAnnotation {
  private MutabilityAnnotation() {
  }
  
  private static final MutabilityAnnotation instance = new MutabilityAnnotation();
  
  public static final IPromiseAnnotation getInstance() {
    return instance;
  }

  static SlotInfo<Boolean> immutableSI;
  
  public static boolean isImmutable(IRNode node) {
    return isXorFalse_filtered(immutableSI, node);
  }
  
  public static void setImmutable(IRNode node, boolean tainted) {    
    // LOG.fine("setting immutable on " + JavaNode.getInfo(node));
    setX_mapped(immutableSI, node, tainted);
  }
  
  /*
   * (non-Javadoc)
   * 
   * @see edu.cmu.cs.fluid.java.bind.AbstractPromiseAnnotation#getRules()
   */
  @Override
  protected IPromiseRule[] getRules() {
    return new IPromiseRule[] {      
      new IPromiseStorage<Boolean>() {
        public String name() {
          return "AbstractImmutable";
        }
        public int type() {
          return BOOL;
        }
        public TokenInfo<Boolean> set(SlotInfo<Boolean> si) {
          immutableSI = si;
          return new TokenInfo<Boolean>(name(), si, name());
        }
        public Operator[] getOps(Class type) {
          return typeDeclOps;
        }        
      },
      new BooleanTagRule("AbstractImmutable", typeDeclOps) {
        @Override
        protected SlotInfo<Boolean> getSI() { return immutableSI; }
      },
    };
  }
}