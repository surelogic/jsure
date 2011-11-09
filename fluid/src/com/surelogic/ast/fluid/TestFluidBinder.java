/*$Header: /cvs/fluid/fluid/src/com/surelogic/ast/fluid/TestFluidBinder.java,v 1.1 2006/07/27 15:28:40 chance Exp $*/
package com.surelogic.ast.fluid;

import com.surelogic.ast.JavaBinder;
import com.surelogic.ast.java.operator.*;

import edu.cmu.cs.fluid.java.bind.IBinder;

public class TestFluidBinder {
  public static void init(IBinder b) {
    synchronized (JavaBinder.class) {
      if (!JavaBinder.hasBinder() || 
          (!(JavaBinder.getBinder() instanceof FluidBinder))) {
        FluidBinder binder = new FluidBinder(b);
        JavaBinder.setBinder(binder);
      }
    }
  }
  
  void test(ICompilationUnitNode cun) {
    
  }
}
