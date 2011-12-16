/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package edu.cmu.cs.fluid.sea;

import java.util.Map;

import com.surelogic.common.refactor.IJavaDeclaration;

import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.sea.ProposedPromiseDrop.Origin;

public interface IProposedPromiseDropInfo extends IDropInfo {
	String getAnnotation();
	String getContents();
	Map<String,String> getAnnoAttributes();
	String getReplacedAnnotation();
	String getReplacedContents();
	Map<String,String> getReplacedAttributes();
	String getJavaAnnotation();
	Origin getOrigin();
	boolean isAbductivelyInferred();
	String getTargetProjectName();
	String getFromProjectName();
	IJavaDeclaration getTargetInfo();
	IJavaDeclaration getFromInfo();
	ISrcRef getAssumptionRef();
	
	long computeHash();
	boolean isSameProposalAs(IProposedPromiseDropInfo i);
}
