/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package edu.cmu.cs.fluid.sea;

import com.surelogic.common.refactor.IJavaDeclaration;

import edu.cmu.cs.fluid.java.ISrcRef;

public interface IProposedPromiseDropInfo extends IDropInfo {
	String getAnnotation();
	String getContents();
	String getJavaAnnotation();
	String getTargetProjectName();
	String getFromProjectName();
	IJavaDeclaration getTargetInfo();
	IJavaDeclaration getFromInfo();
	ISrcRef getAssumptionRef();
	boolean isSameProposalAs(IProposedPromiseDropInfo i);
}
