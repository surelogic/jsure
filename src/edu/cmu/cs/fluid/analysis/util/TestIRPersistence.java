package edu.cmu.cs.fluid.analysis.util;

import java.io.IOException;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.JavaModelException;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.util.FileLocator;

public class TestIRPersistence extends AbstractIRAnalysisModule {
  static final Bundle b         = JJNode.getBundle();
  static final FileLocator floc = IRPersistent.fluidFileLocator;
  
  @Override
  public void analyzeBegin(IProject p) {
    super.analyzeBegin(p);
  }
  
  @Override
  protected void doAnalysisOnAFile(IRNode cu) throws JavaModelException {
    IRRegion r = new IRRegion();
    for(IRNode n : JJNode.tree.topDown(cu)) {
      r.saveNode(n);
    }
    IRChunk c        = r.createChunk(b);
    try {
      String msg = DebugUnparser.toString(cu);
      System.out.println("Storing "+msg);
      System.out.println("As chunk "+c);
      c.store(floc);
      c.unload();
      System.out.println("Loading "+msg);
      c.load(floc);
      System.out.println("Done with "+DebugUnparser.toString(cu));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
