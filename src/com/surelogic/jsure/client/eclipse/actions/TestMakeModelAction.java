package com.surelogic.jsure.client.eclipse.actions;

import org.eclipse.jdt.core.*;
import org.eclipse.jface.action.IAction;

import com.surelogic.common.core.JDTUtility;
import com.surelogic.common.core.jobs.EclipseJob;
import com.surelogic.common.jobs.*;
import com.surelogic.common.ui.actions.AbstractMainAction;
import com.surelogic.jsure.core.xml.PromisesXMLBuilder;
import com.surelogic.xml.*;

public class TestMakeModelAction extends AbstractMainAction {
	@Override
	public void run(IAction action) {
		SLJob job = new AbstractSLJob("Testing makeModel()") {			
			@Override
			public SLStatus run(SLProgressMonitor monitor) {
				try {
					for(IJavaProject p : JDTUtility.getJavaProjects()) {
						for(IPackageFragment pf : p.getPackageFragments()) {
							for(ICompilationUnit cu : pf.getCompilationUnits()) {
								IType t = cu.findPrimaryType();
								handleIType(pf, t);
							}
							for(IClassFile cf : pf.getClassFiles()) {
								IType t = cf.findPrimaryType();
								handleIType(pf, t);
							}
						}
					}
				} catch(JavaModelException e) {
					e.printStackTrace();
				}
				return SLStatus.OK_STATUS;
			}
			
			void handleIType(IPackageFragment pf, IType t) throws JavaModelException {
				if (t.isAnonymous()) {
					return;
				}
				PackageElement pe = PromisesXMLBuilder.makeModel(t);
				if (pe == null) {
					return;
				}
				ClassElement ce = pe.getClassElement();
				boolean first = true;
				if (ce != null) {
					for(MethodElement m : ce.getMethods()) {
						if (m.getParams().length() == 0) {
							continue; // Skip no-args methods
						}
						if (first) {
							System.out.println("Class: "+ce.getLabel());							
							if (pe.getName().equals("java.util") && ce.getName().equals("Map")) {
								System.out.println();
							}							
							first = false;
						}
						System.out.println("\t"+m.getLabel());
					}
				}
			}
		};
		EclipseJob.getInstance().schedule(job);
	}
}
