/*$Header: /cvs/fluid/fluid/src/com/surelogic/test/WarningDropOutput.java,v 1.13 2008/05/15 16:24:11 aarong Exp $*/
package com.surelogic.test;

import com.surelogic.dropsea.ir.Category;
import com.surelogic.dropsea.ir.IRReferenceDrop;
import com.surelogic.dropsea.ir.InfoDrop;
import com.surelogic.dropsea.ir.ModelingProblemDrop;

public class WarningDropOutput extends AbstractTestOutput {
  private final Category category;
  
  public WarningDropOutput(String name) {
    super(name);
    category = name == null ? null : Category.getResultInstance(name);
//    System.out.println("Creating WarningDropOutput: "+name);
    //new Throwable("Creating WarningDropOutput: "+name).printStackTrace();
  }
  
  public WarningDropOutput() {
    this(null);
  }
  
  private IRReferenceDrop newDrop(ITest o, String msg) {
    return newDrop(o, msg, false);
  }
  
  private IRReferenceDrop newDrop(ITest o, String msg, boolean success) {
    IRReferenceDrop drop = success ? new InfoDrop(o.getNode()) : new ModelingProblemDrop(o.getNode());
    drop.setMessage(12, msg);
    if (category != null) {
      drop.setCategory(null);
    }
//    if (o.getNode() != null) {
//      drop.setNodeAndCompilationUnitDependency(o.getNode());
//    }
    return drop;
  }
  
  public void reportError(ITest o, Throwable ex) {
    if (report(o, ex)) {    
      // FIX store the exception   
      // FIX PromiseWarningDrop drop = 
      newDrop(o, ex.getMessage()); 
    }
  }

  public void reportFailure(ITest o, String msg) {
    if (report(o, msg)) {
      newDrop(o, msg);
    }
  }

  public void reportSuccess(ITest o, String msg) {
    if (report(o, msg)) {
      newDrop(o, msg, true);
    }
  }
  
  //public static final ITestOutput prototype = new WarningDropOutput("Test Results");
  
  public static final ITestOutputFactory factory = new ITestOutputFactory() {
    public ITestOutput create(String name) {
      return new WarningDropOutput(name);
    }
  };
}
