package com.surelogic.jsure.client.eclipse.model.selection;

import org.eclipse.swt.graphics.Image;

public interface ISelectionFilterFactory extends
		Comparable<ISelectionFilterFactory> {

	/**
	 * Constructs a filter object within the passed selection operating on the
	 * output of the passed previous filter.
	 * 
	 * @param selection
	 *            a selection.
	 * @param previous
	 *            a filter within the selection to operate on the output of, or
	 *            <code>null</code> if no previous filter (i.e., this is the
	 *            first filter within the selection).
	 * @return the new filter.
	 */
	Filter construct(final Selection selection, final Filter previous);

	/**
	 * Gets the user interface label for this filter.
	 * 
	 * @return the user interface label for this filter.
	 */
	String getFilterLabel();

	/**
	 * Gets the user interface image for this filter.
	 * 
	 * @return the user interface image for this filter.
	 */
	Image getFilterImage();
}
