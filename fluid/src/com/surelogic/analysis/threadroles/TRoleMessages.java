/*
 * Created on Nov 16, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.surelogic.analysis.threadroles;

import java.util.Iterator;

import com.surelogic.dropsea.IHintDrop;
import com.surelogic.dropsea.ir.Drop;
import com.surelogic.dropsea.ir.IRReferenceDrop;
import com.surelogic.dropsea.ir.HintDrop;
import com.surelogic.dropsea.ir.ResultDrop;
import com.surelogic.dropsea.ir.drops.threadroles.TRoledClassDrop;

import edu.cmu.cs.fluid.ir.IRNode;

/**
 * @author dfsuther
 * 
 *         TODO To change the template for this generated type comment go to
 *         Window - Preferences - Java - Code Generation - Code and Comments
 */
public class TRoleMessages {
  public static final String assuranceCategory = "Thread role assurances";

  public static final String warningCategory = "Thread role warnings";

  public static final String problemCategory = "Thread role problems";

  public static final String infoCategory = "Thread role inferences";

  public static final String multiThreadedInfoCategory = "Possibly Multi-threaded methods";

  public static ResultDrop createResultDrop(String msg, String resultDropKind, IRNode loc) {
    ResultDrop rd = new ResultDrop(loc); // TODO FIX TOP LEVEL
    rd.setConsistent();
    // rd.addCheckedPromise(pd);
    // rd.setNodeAndCompilationUnitDependency(loc);
    rd.setMessage(msg);
    rd.setCategorizingMessage(assuranceCategory);

    if (loc != null) {
      Drop d = TRoledClassDrop.getTRoleClassDrop(loc);
      if (d != null) {
        d.addDependent(rd);
      }
    }
    return rd;
  }

  public static HintDrop createWarningDrop(String msg, IRNode loc) {
    HintDrop wd = HintDrop.newWarning(loc);
    // rd.addCheckedPromise(pd);
    // wd.setNodeAndCompilationUnitDependency(loc);
    wd.setMessage(msg);
    wd.setCategorizingMessage(warningCategory);

    if (loc != null) {
      Drop d = TRoledClassDrop.getTRoleClassDrop(loc);
      if (d != null) {
        d.addDependent(wd);
      }
    }
    return wd;
  }

  public static HintDrop createInfoDrop(String msg, IRNode loc) {
    HintDrop id = HintDrop.newInformation(loc);
    // rd.addCheckedPromise(pd);
    // id.setNodeAndCompilationUnitDependency(loc);
    id.setMessage(msg);
    id.setCategorizingMessage(infoCategory);

    if (loc != null) {
      Drop d = TRoledClassDrop.getTRoleClassDrop(loc);
      if (d != null) {
        d.addDependent(id);
      }
    }

    return id;
  }

  public static ResultDrop createProblemDrop(String msg, String resultDropKind, IRNode loc) {
    ResultDrop rd = new ResultDrop(loc); // TODO FIX TOP LEVEL
    rd.setInconsistent();
    // rd.addCheckedPromise(pd);
    if (loc != null) {
      // rd.setNodeAndCompilationUnitDependency(loc);
      if (loc != null) {
        Drop d = TRoledClassDrop.getTRoleClassDrop(loc);
        if (d != null) {
          d.addDependent(rd);
        }
      }
    }
    rd.setMessage(msg);
    rd.setCategorizingMessage(problemCategory);

    return rd;
  }

  public static void invalidateDrops() {
    {
      Iterator<HintDrop> it = com.surelogic.dropsea.ir.Sea.getDefault().getDropsOfExactType(HintDrop.class).iterator();

      while (it.hasNext()) {
        HintDrop d = it.next();
        if (d.getHintType() == IHintDrop.HintType.WARNING && warningCategory.equals(d.getCategorizingMessage())) {
          d.invalidate();
        }
      }
    }
    {
      Iterator<ResultDrop> it = com.surelogic.dropsea.ir.Sea.getDefault().getDropsOfExactType(ResultDrop.class).iterator();

      while (it.hasNext()) {
        ResultDrop d = (ResultDrop) it.next();
        if (problemCategory.equals(d.getCategorizingMessage())) {
          d.invalidate();
        }
      }
    }
  }

  public static IRReferenceDrop createOutputDrop(String desiredCategory, String msg, IRNode loc) {
    if (desiredCategory == assuranceCategory) {
      return createResultDrop(msg, "TODO: fill me in", loc);
    } else if (desiredCategory.equals(infoCategory)) {
      return createInfoDrop(msg, loc);
    } else if (desiredCategory.equals(warningCategory)) {
      return createWarningDrop(msg, loc);
    } else if (desiredCategory.equals(problemCategory)) {
      return createProblemDrop(msg, "TODO: Fill Me In", loc);
    } else {
      return null;
    }
  }
}
