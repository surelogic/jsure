/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package com.surelogic.analysis;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.java.*;
import edu.cmu.cs.fluid.java.bind.*;
import edu.cmu.cs.fluid.java.util.VisitUtil;

public class JavaProjects {
	private static final IRObjectType<IIRProject> projectType = new IRObjectType<IIRProject>();
	protected static final SlotInfo<IIRProject> projectSI = 
		JavaNode.getVersionedSlotInfo(JavaProjects.class.getName(), projectType);
	
	public static IIRProject getProject(IRNode cu) {
		if (cu == null || !cu.valueExists(projectSI)) {
			return null;
		}
		return cu.getSlotValue(projectSI);
	}
	
	public static IIRProject getEnclosingProject(IRNode here) {
		//This doesn't use getPromisedFor
		//final IRNode cu = VisitUtil.findRoot(here);
		final IRNode cu = VisitUtil.findCompilationUnit(here);
		//final IRNode cu = VisitUtil.getEnclosingCompilationUnit(here);
		return getProject(cu);
	}
	
	public static IIRProject getEnclosingProject(IJavaType t) {
		if (t instanceof IJavaDeclaredType) {
			IJavaDeclaredType dt = (IJavaDeclaredType) t;
			return getEnclosingProject(dt.getDeclaration());
		}
		if (t instanceof IJavaTypeFormal) {
			IJavaTypeFormal dt = (IJavaTypeFormal) t;
			return getEnclosingProject(dt.getDeclaration());
		}
		return null;
	}
	
	public static void setProject(IRNode cu, IIRProject p) {
		if (p == null) {
			return;
		}
		/*
		if (p.getName().equals("Util")) {
			String name = JavaNames.genPrimaryTypeName(cu);
			if (name != null) {
				System.out.println("Marking as in Util: "+JavaNames.genPrimaryTypeName(cu));
				System.out.println();
			}
		}
		*/
		// HACK until arrayType is cloned
		IIRProject old = getProject(cu);
		if (old == null) {
			cu.setSlotValue(projectSI, p);
		} 
		else if (old != p) {
			SLLogger.getLogger().warning("Ignored attempt to reset project from "+old.getName()+" to "+p.getName()+" for "+DebugUnparser.toString(cu));
		}
	}
}
