/*$Header: /cvs/fluid/fluid/src/com/surelogic/sea/drops/BooleanPromiseDrop.java,v 1.2 2007/06/27 14:37:40 chance Exp $*/
package edu.cmu.cs.fluid.sea.drops;

import com.surelogic.aast.*;

import edu.cmu.cs.fluid.sea.PromiseDrop;

/**
 * Using the OLD drop-sea
 * 
 * @author edwin
 */
public class BooleanPromiseDrop<A extends IAASTRootNode> extends PromiseDrop<A> {
  public BooleanPromiseDrop(A a) {
    super(a);
  }
}
