package edu.cmu.cs.fluid.analysis.util;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.JavaModelException;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.SlotInfo;

public class DeleteIR extends AbstractIRAnalysisModule {
  public DeleteIR() {
  }

  @Override
  protected void doAnalysisOnAFile(IRNode cu) throws JavaModelException {
    //Binding.deleteCU(cu);
  }
  @Override
  protected Iterable<IRNode> finishAnalysis(IProject project) {
    SlotInfo.gc();
    return NONE_TO_ANALYZE;
  }
}
