/*
 * Created on Nov 16, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.surelogic.analysis.colors;

import java.util.Iterator;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.sea.Category;
import edu.cmu.cs.fluid.sea.Drop;
import edu.cmu.cs.fluid.sea.IRReferenceDrop;
import edu.cmu.cs.fluid.sea.InfoDrop;
import edu.cmu.cs.fluid.sea.ResultDrop;
import edu.cmu.cs.fluid.sea.WarningDrop;
import edu.cmu.cs.fluid.sea.drops.threadroles.TRoleResultDrop;
import edu.cmu.cs.fluid.sea.drops.threadroles.TRoledClassDrop;


/**
 * @author dfsuther
 * 
 * TODO To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Generation - Code and Comments
 */
public class ColorMessages {
  public static final Category assuranceCategory = Category
  .getResultInstance("Thread color assurances");

public static final Category warningCategory = Category
  .getResultInstance("Thread color warnings");

public static final Category problemCategory = Category
  .getResultInstance("Thread color problems");

public static final Category infoCategory = Category
  .getResultInstance("Thread color inferences");

public static final Category multiThreadedInfoCategory = 
  Category.getResultInstance("Possibly Multi-threaded methods");

  public static ResultDrop createResultDrop(String msg, String resultDropKind, IRNode loc) {
    ResultDrop rd = new TRoleResultDrop(resultDropKind);
    rd.setConsistent();
    // rd.addCheckedPromise(pd);
    rd.setNodeAndCompilationUnitDependency(loc);
    rd.setMessage(msg);
    rd.setCategory(assuranceCategory);

    if (loc != null) {
      Drop d = TRoledClassDrop.getTRoleClassDrop(loc);
      if (d != null) {
        d.addDependent(rd);
      }
    }
    return rd;
  }

  public static WarningDrop createWarningDrop(String msg,
      IRNode loc) {
    WarningDrop wd = new WarningDrop("ColorMessages");
    // rd.addCheckedPromise(pd);
    wd.setNodeAndCompilationUnitDependency(loc);
    wd.setMessage(msg);
    wd.setCategory(warningCategory);

    if (loc != null) {
      Drop d = TRoledClassDrop.getTRoleClassDrop(loc);
      if (d != null) {
        d.addDependent(wd);
      }
    }
    return wd;
  }
  

  public static InfoDrop createInfoDrop(String msg, IRNode loc) {
    InfoDrop id = new InfoDrop(null);
    // rd.addCheckedPromise(pd);
    id.setNodeAndCompilationUnitDependency(loc);
    id.setMessage(msg);
    id.setCategory(infoCategory);

    if (loc != null) {
      Drop d = TRoledClassDrop.getTRoleClassDrop(loc);
      if (d != null) {
        d.addDependent(id);
      }
    }
    
    return id;
  }

  public static ResultDrop createProblemDrop(String msg,
      String resultDropKind, IRNode loc) {
    ResultDrop rd = new ResultDrop(resultDropKind);
    rd.setInconsistent();
    // rd.addCheckedPromise(pd);
    if (loc != null) {
      rd.setNodeAndCompilationUnitDependency(loc);
      if (loc != null) {
        Drop d = TRoledClassDrop.getTRoleClassDrop(loc);
        if (d != null) {
          d.addDependent(rd);
        }
      }
    }
    rd.setMessage(msg);
    rd.setCategory(problemCategory);

    return rd;
  }


  public static void invalidateDrops() {
    Iterator it = edu.cmu.cs.fluid.sea.Sea.getDefault()
        .getDropsOfExactType(WarningDrop.class).iterator();

    while (it.hasNext()) {
      WarningDrop d = (WarningDrop) it.next();
      if (d.getCategory() == warningCategory) {
        d.invalidate();
      }
    }
    it = edu.cmu.cs.fluid.sea.Sea.getDefault()
        .getDropsOfExactType(ResultDrop.class).iterator();

    while (it.hasNext()) {
      ResultDrop d = (ResultDrop) it.next();
      if (d.getCategory() == problemCategory) {
        d.invalidate();
      }
    }
  }
  
  public static IRReferenceDrop createOutputDrop(Category desiredCategory, String msg,
      IRNode loc) {
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

