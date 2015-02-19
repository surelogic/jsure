package com.surelogic.dropsea;

import java.util.Map;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
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

  /**
   * An indication of how a proposal was generated or originated.
   */
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

  /**
   * Gets the Java annotation being proposed. For
   * <code>@Starts("nothing")</code> the value of this string would be
   * {@code "Starts"}.
   * 
   * @return the Java annotation being proposed.
   */
  @NonNull
  String getAnnotation();

  /**
   * Gets the value of the Java annotation being proposed. For
   * <code>@Starts("nothing")</code> the value of this string would be
   * {@code "nothing"}. For <code>@Borrowed</code>, which has no value, this
   * string would be {@code null}. The contents placed into this string should
   * not be escaped. Any embedded quotations or backward slashes will be escaped
   * before output.
   * 
   * @return the value of the Java annotation being proposed.
   */
  @Nullable
  String getValue();

  /**
   * Gets the non-value attributes for the Java annotation being proposed. The
   * map is from the name of the attribute to the value.
   * 
   * @return non-value attributes for the Java annotation being proposed. May be
   *         empty.
   */
  @NonNull
  Map<String, String> getAttributes();

  /**
   * Gets the Java annotation being replaced. For
   * <code>@Starts("nothing")</code> the value of this string would be
   * {@code "Starts"}.
   * 
   * @return the Java annotation being replaced, or {@code null} if no
   *         annotation is being replaced.
   */
  @Nullable
  String getReplacedAnnotation();

  /**
   * Gets the value of the Java annotation being replaced. For
   * <code>@Starts("nothing")</code> the value of this string would be
   * {@code "nothing"}. For <code>@Borrowed</code>, which has no value, this
   * string would be {@code null}. The contents placed into this string should
   * not be escaped. Any embedded quotations or backward slashes will be escaped
   * before output.
   * 
   * @return the value of the Java annotation being replaced, or {@code null} if
   *         no annotation is being replaced.
   */
  @Nullable
  String getReplacedValue();

  /**
   * Gets the non-value attributes for the Java annotation being replaced. The
   * map is from the name of the attribute to the value.
   * 
   * @return non-value attributes for the Java annotation being replaced. May be
   *         empty.
   */
  @NonNull
  Map<String, String> getReplacedAttributes();

  /**
   * Gets a reference to the Java code this proposed promise was proposed from.
   * This location could be used to generate an assumption, instead the actual
   * proposal, if, for example, the proposal is about a binary.
   * 
   * @return a reference to the Java code this proposed promise was proposed
   *         from, cannot be {@code null} for a proposed promise drop.
   */
  @NonNull
  IJavaRef getAssumptionRef();

  /**
   * Gets the annotation as it would appear in Java source code. The value, if
   * any, is escaped.
   * 
   * @return the annotation as it would appear in Java source code.
   */
  @NonNull
  String getJavaAnnotation();

  /**
   * Gets an indication of how this proposal was generated. One of
   * {@link Origin#CODE}, {@link Origin#MODEL}, {@link Origin#PROBLEM}.
   * 
   * @return an indication of how this proposal was generated.
   */
  @NonNull
  Origin getOrigin();

  /**
   * Is this proposed promise inferred from an existing user annotation or
   * model.
   * 
   * @return {@code true} if this proposed promise inferred from an existing
   *         user annotation or model, {@code false} if this proposal was
   *         inferred from code with no model/annotation basis for it
   *         whatsoever.
   */
  boolean isAbductivelyInferred();
}
