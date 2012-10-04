package com.surelogic.dropsea;

import java.util.Map;

import com.surelogic.common.ref.IJavaRef;
import com.surelogic.common.refactor.IJavaDeclaration;
import com.surelogic.dropsea.ir.ProposedPromiseDrop.Origin;

/**
 * The interface for all proposed promise drops within the sea, intended to
 * allow multiple implementations. A verifying analysis uses this type to
 * propose to the tool user that an annotation might be needed in the code based
 * upon abductive inference.
 * <p>
 * The verifying analyses use the IR drop-sea and the Eclipse client loads
 * snapshots using the IR-free drop-sea.
 */
public interface IProposedPromiseDrop extends IAnalysisOutputDrop {

  String getAnnotation();

  String getContents();

  Map<String, String> getAnnoAttributes();

  String getReplacedAnnotation();

  String getReplacedContents();

  Map<String, String> getReplacedAttributes();

  String getJavaAnnotation();

  Origin getOrigin();

  boolean isAbductivelyInferred();

  String getTargetProjectName();

  String getFromProjectName();

  IJavaDeclaration getTargetInfo();

  IJavaDeclaration getFromInfo();

  IJavaRef getAssumptionRef();

  long computeHash();

  boolean isSameProposalAs(IProposedPromiseDrop i);
}
