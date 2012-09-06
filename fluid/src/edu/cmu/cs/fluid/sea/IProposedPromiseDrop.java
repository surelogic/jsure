package edu.cmu.cs.fluid.sea;

import java.util.Map;

import com.surelogic.common.refactor.IJavaDeclaration;

import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.sea.ProposedPromiseDrop.Origin;

public interface IProposedPromiseDrop extends IDrop {
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
	boolean isSameProposalAs(IProposedPromiseDrop i);
}
