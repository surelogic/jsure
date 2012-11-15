package com.surelogic.jsure.client.eclipse.model.java;

import org.eclipse.jface.viewers.IContentProvider;

import com.surelogic.Nullable;
import com.surelogic.dropsea.ScanDifferences;

/**
 * Used to communicate with {@link Element} instances about the difference
 * preferences of the view the model is being used by. In most cases this
 * interface should be implemented by the {@link IContentProvider} for the view.
 */
public interface IViewDiffState {

  /**
   * Gets the scan differences for all elements, {@code null} if none no scan
   * difference information is available.
   * 
   * @return the scan differences for all elements, {@code null} if none no scan
   *         difference information is available.
   */
  @Nullable
  ScanDifferences getScanDifferences();

  /**
   * Gets if scan differences should be highlighted.
   * 
   * @return {@code true} if scan differences should be highlighted in the tree,
   *         {@code false} if not.
   */
  boolean highlightDifferences();
}
