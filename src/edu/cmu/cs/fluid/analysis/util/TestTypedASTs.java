package edu.cmu.cs.fluid.analysis.util;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.JavaModelException;

import com.surelogic.ast.fluid.TestFluidBinder;
import com.surelogic.ast.java.operator.*;

import edu.cmu.cs.fluid.eclipse.Eclipse;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.TestVisitor;

public class TestTypedASTs extends AbstractIRAnalysisModule {
  final TestPromiseNodeVisitor<Object> nv = new TestPromiseNodeVisitor<Object>(true);
  TestVisitor tv;
  long irTime;
  long astTime;
  
  @Override
  public void analyzeBegin(IProject p) {
    super.analyzeBegin(p);
    
    IBinder b = Eclipse.getDefault().getTypeEnv(p).getBinder();
    tv = new TestVisitor(b, true);
    TestFluidBinder.init(b);
  }
  
  @Override
  protected boolean useTypedASTs() {
    return true;
  }
  
  @Override
  protected void doAnalysisOnAFile(IRNode cu) throws JavaModelException {
    throw new UnsupportedOperationException();
  }
  
  @Override
  protected void doAnalysisOnAFile(IRNode n, ICompilationUnitNode cu)
  throws JavaModelException 
  {     
    if (cu == null) {
      System.out.println("cu is null");
      return;
    }
    cu.accept(nv);
    long startAST = System.nanoTime();
    cu.accept(nv);
    long ast = System.nanoTime();
    tv.doAccept(n);
    long start = System.nanoTime();
    tv.doAccept(n);
    long ir  = System.nanoTime();
    irTime  += (ir - start);
    astTime += (ast - startAST);
    System.out.println("IR run time so far:    \t"+irTime);
    System.out.println("Proxy run time so far: \t"+astTime);
  }
}
