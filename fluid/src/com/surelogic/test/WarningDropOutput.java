/*$Header: /cvs/fluid/fluid/src/com/surelogic/test/WarningDropOutput.java,v 1.13 2008/05/15 16:24:11 aarong Exp $*/
package com.surelogic.test;

import com.surelogic.dropsea.ir.Drop;
import com.surelogic.dropsea.ir.HintDrop;
import com.surelogic.dropsea.ir.ModelingProblemDrop;

public class WarningDropOutput extends AbstractTestOutput {
  private final String category;

  public WarningDropOutput(String name) {
    super(name);
    category = name;
    // System.out.println("Creating WarningDropOutput: "+name);
    // new Throwable("Creating WarningDropOutput: "+name).printStackTrace();
  }

  public WarningDropOutput() {
    this(null);
  }

  private Drop newDrop(ITest o, String msg) {
    return newDrop(o, msg, false);
  }

  private Drop newDrop(ITest o, String msg, boolean success) {
    Drop drop = success ? HintDrop.newInformation(o.getNode()) : new ModelingProblemDrop(o.getNode());
    drop.setMessage(msg);
    if (category != null) {
      drop.setCategorizingMessage(null);
    }
    // if (o.getNode() != null) {
    // drop.setNodeAndCompilationUnitDependency(o.getNode());
    // }
    return drop;
  }

  @Override
  public void reportError(ITest o, Throwable ex) {
    if (report(o, ex)) {
      // FIX store the exception
      // FIX PromiseWarningDrop drop =
      newDrop(o, ex.getMessage());
    }
  }

  @Override
  public void reportFailure(ITest o, String msg) {
    if (report(o, msg)) {
      newDrop(o, msg);
    }
  }

  @Override
  public void reportSuccess(ITest o, String msg) {
    if (report(o, msg)) {
      newDrop(o, msg, true);
    }
  }

  // public static final ITestOutput prototype = new
  // WarningDropOutput("Test Results");

  public static final ITestOutputFactory factory = new ITestOutputFactory() {
    @Override
    public ITestOutput create(String name) {
      return new WarningDropOutput(name);
    }
  };
}
