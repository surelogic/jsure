package com.surelogic.dropsea;

import java.util.Collection;

import com.surelogic.NonNull;

/**
 * The interface for for all promises, or assertion, drops within the sea,
 * intended to allow multiple implementations.
 * <p>
 * The verifying analyses use the IR drop-sea and the Eclipse client loads
 * snapshots using the IR-free drop-sea.
 */
public interface IPromiseDrop extends IProofDrop {

  /**
   * Gets if this promise is assumed.
   * 
   * @return <code>true</code> if the promise is assumed, <code>false</code>
   *         otherwise.
   */
  boolean isAssumed();

  /**
   * Returns if this PromiseDrop has been checked by analysis or not. If this
   * PromiseDrop (or any deponent PromiseDrop of this PromiseDrop) has results
   * it is considered checked, otherwise it is considered trusted. This approach
   * is designed to allow the system to detect when a PromiseDrop that usually
   * is checked by an analysis has not been checked (i.e., the analysis is
   * turned off). This method is intended to be overridden by subclasses where
   * the default behavior is wrong. That said, however, usually the subclass
   * should override {@link #isIntendedToBeCheckedByAnalysis()} and note that
   * the promise is not intended to be checked by analysis (which will cause
   * this method to return <code>true</code>).
   * <p>
   * We currently trust XML promises as having been checked by analysis, or
   * defined by the JLS.
   * 
   * @return <code>true</code> if the PromiseDrop is considered checked by
   *         analysis, <code>false</code> otherwise.
   */
  boolean isCheckedByAnalysis();

  /**
   * Returns if this PromiseDrop is <i>intended</i> to be checked by analysis or
   * not. Most promises are supported by analysis results (i.e., they have
   * ResultDrops attached to them), however some are simply well-formed (e.g.,
   * region models). If the promise is simply well-formed then it should
   * override this method and return <code>false</code>.
   * 
   * @return <code>true</code> if the PromiseDrop is intended to be checked by
   *         analysis, <code>false</code> otherwise.
   */
  boolean isIntendedToBeCheckedByAnalysis();

  /**
   * Gets if this promise is virtual.
   * 
   * @return <code>true</code> if the promise is virtual, <code>false</code>
   *         otherwise.
   */
  boolean isVirtual();

  /**
   * Returns a copy of the set of result drops which directly check this promise
   * drop.
   * 
   * @return a non-null (possibly empty) set which check this promise drop
   */
  @NonNull
  Collection<? extends IAnalysisResultDrop> getCheckedBy();

  /**
   * Gets the set of promise drops that are a dependent of this promise drop.
   * 
   * @return the set of promise drops that are a dependent of this promise drop.
   */
  @NonNull
  Collection<? extends IPromiseDrop> getDependentPromises();

  /**
   * Gets the set of promise drops that are a deponent of this promise drop.
   * 
   * @return the set of promise drops that are a deponent of this promise drop.
   */
  @NonNull
  Collection<? extends IPromiseDrop> getDeponentPromises();
  
  boolean showAtTopLevel();
  
  boolean placeInASubFolder();
}
