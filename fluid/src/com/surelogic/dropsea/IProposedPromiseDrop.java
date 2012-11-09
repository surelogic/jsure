package com.surelogic.dropsea;

import java.util.Map;

import com.surelogic.NonNull;
import com.surelogic.common.ref.IJavaRef;

/**
 * The interface for all proposed promise drops within the sea, intended to
 * allow multiple implementations. A verifying analysis uses this type to
 * propose to the tool user that an annotation might be needed in the code based
 * upon abductive inference.
 * <p>
 * The verifying analyses use the IR drop-sea and the Eclipse client loads
 * snapshots using the IR-free drop-sea.
 */
public interface IProposedPromiseDrop extends IAnalysisOutputDrop, ISnapshotDrop {

  public enum Origin {
    /**
     * This proposal was inferred from code with no model/annotation basis for
     * it whatsoever.
     */
    CODE,
    /**
     * This proposal was inferred from code and a model. It could be extending
     * or augmenting an existing model based upon the program's implementation.
     */
    MODEL,
    /**
     * This proposed promise was created to help fix a modeling problem.
     */
    PROBLEM
  }

  @NonNull
  IJavaRef getJavaRef();

  @NonNull
  IJavaRef getAssumptionRef();

  String getAnnotation();

  String getContents();

  @NonNull
  Map<String, String> getAnnoAttributes();

  String getReplacedAnnotation();

  String getReplacedContents();

  @NonNull
  Map<String, String> getReplacedAttributes();

  String getJavaAnnotation();

  Origin getOrigin();

  boolean isAbductivelyInferred();

}
