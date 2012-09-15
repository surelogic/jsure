/*
 * Created on Nov 16, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.surelogic.analysis.threadroles;

import java.util.Iterator;

import com.surelogic.dropsea.InfoDropLevel;
import com.surelogic.dropsea.ir.Category;
import com.surelogic.dropsea.ir.Drop;
import com.surelogic.dropsea.ir.IRReferenceDrop;
import com.surelogic.dropsea.ir.InfoDrop;
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
	public static final Category assuranceCategory = Category
			.getResultInstance("Thread role assurances");

	public static final Category warningCategory = Category
			.getResultInstance("Thread role warnings");

	public static final Category problemCategory = Category
			.getResultInstance("Thread role problems");

	public static final Category infoCategory = Category
			.getResultInstance("Thread role inferences");

	public static final Category multiThreadedInfoCategory = Category
			.getResultInstance("Possibly Multi-threaded methods");

	public static ResultDrop createResultDrop(String msg,
			String resultDropKind, IRNode loc) {
		ResultDrop rd = new ResultDrop(loc); // TODO FIX TOP LEVEL
		rd.setConsistent();
		// rd.addCheckedPromise(pd);
		//rd.setNodeAndCompilationUnitDependency(loc);
		rd.setMessage(12, msg);
		rd.setCategory(assuranceCategory);

		if (loc != null) {
			Drop d = TRoledClassDrop.getTRoleClassDrop(loc);
			if (d != null) {
				d.addDependent(rd);
			}
		}
		return rd;
	}

	public static InfoDrop createWarningDrop(String msg, IRNode loc) {
		InfoDrop wd = new InfoDrop(loc, InfoDropLevel.WARNING);
		// rd.addCheckedPromise(pd);
	//	wd.setNodeAndCompilationUnitDependency(loc);
		wd.setMessage(12, msg);
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
		InfoDrop id = new InfoDrop(loc);
		// rd.addCheckedPromise(pd);
		//id.setNodeAndCompilationUnitDependency(loc);
		id.setMessage(12, msg);
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
		ResultDrop rd = new ResultDrop(loc); // TODO FIX TOP LEVEL
		rd.setInconsistent();
		// rd.addCheckedPromise(pd);
		if (loc != null) {
			//rd.setNodeAndCompilationUnitDependency(loc);
			if (loc != null) {
				Drop d = TRoledClassDrop.getTRoleClassDrop(loc);
				if (d != null) {
					d.addDependent(rd);
				}
			}
		}
		rd.setMessage(12, msg);
		rd.setCategory(problemCategory);

		return rd;
	}

	public static void invalidateDrops() {
		{
			Iterator<InfoDrop> it = com.surelogic.dropsea.ir.Sea.getDefault()
					.getDropsOfExactType(InfoDrop.class).iterator();

			while (it.hasNext()) {
				InfoDrop d = it.next();
				if (d.getLevel() == InfoDropLevel.WARNING && d.getCategory() == warningCategory) {
					d.invalidate();
				}
			}
		}
		{
			Iterator<ResultDrop> it = com.surelogic.dropsea.ir.Sea.getDefault()
					.getDropsOfExactType(ResultDrop.class).iterator();

			while (it.hasNext()) {
				ResultDrop d = (ResultDrop) it.next();
				if (d.getCategory() == problemCategory) {
					d.invalidate();
				}
			}
		}
	}

	public static IRReferenceDrop createOutputDrop(Category desiredCategory,
			String msg, IRNode loc) {
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
