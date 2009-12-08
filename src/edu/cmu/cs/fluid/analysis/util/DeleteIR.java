package edu.cmu.cs.fluid.analysis.util;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.JavaModelException;

import com.surelogic.analysis.IAnalysisMonitor;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.SlotInfo;

public class DeleteIR extends AbstractIRAnalysisModule {
  public DeleteIR() {
  }

  @Override
  protected boolean doAnalysisOnAFile(IRNode cu, IAnalysisMonitor monitor) throws JavaModelException {
    //Binding.deleteCU(cu);
	  return false;
  }
  @Override
  protected Iterable<IRNode> finishAnalysis(IProject project, IAnalysisMonitor monitor) {
    SlotInfo.gc();
    return NONE_TO_ANALYZE;
  }
}
