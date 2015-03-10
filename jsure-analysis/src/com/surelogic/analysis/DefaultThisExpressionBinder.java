/*$Header: /cvs/fluid/fluid/src/com/surelogic/analysis/DefaultThisExpressionBinder.java,v 1.1 2008/01/18 23:10:45 aarong Exp $*/
package com.surelogic.analysis;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;

public final class DefaultThisExpressionBinder extends AbstractThisExpressionBinder {
  public DefaultThisExpressionBinder(final IBinder b) {
    super(b);
  }

  @Override
  protected IRNode bindReceiver(final IRNode node) {
    return defaultBindReceiver(node);
  }

  @Override
  protected IRNode bindQualifiedReceiver(final IRNode outerType, final IRNode node) {
    return defaultBindQualifiedReceiver(outerType, node);
  }
}
