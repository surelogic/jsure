package com.surelogic.dropsea;

import java.util.Map;

import com.surelogic.common.refactor.IJavaDeclaration;
import com.surelogic.dropsea.ir.ProposedPromiseDrop.Origin;

import edu.cmu.cs.fluid.java.ISrcRef;

/**
 * The interface for the base class for all proposed promise drops within the
 * sea, intended to allow multiple implementations. The analysis uses the IR
 * drop-sea and the Eclipse client loads snapshots using a IR-free drop-sea.
 */
public interface IProposedPromiseDrop extends IDrop {

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

  ISrcRef getAssumptionRef();

  long computeHash();

  boolean isSameProposalAs(IProposedPromiseDrop i);
}
