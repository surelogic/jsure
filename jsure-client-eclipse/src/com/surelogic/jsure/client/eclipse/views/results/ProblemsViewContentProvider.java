package com.surelogic.jsure.client.eclipse.views.results;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.eclipse.swt.graphics.Image;

import com.surelogic.common.CommonImages;
import com.surelogic.common.ui.SLImages;
import com.surelogic.jsure.core.preferences.ModelingProblemFilterUtility;
import com.surelogic.jsure.core.scans.JSureDataDirHub;
import com.surelogic.jsure.core.scans.JSureScanInfo;

import edu.cmu.cs.fluid.sea.IDropInfo;
import edu.cmu.cs.fluid.sea.PromiseWarningDrop;

public final class ProblemsViewContentProvider extends
		AbstractResultsTableContentProvider<IDropInfo> {

	ProblemsViewContentProvider() {
		super("Description");
	}

	protected String getAndSortResults(List<IDropInfo> contents) {
		final JSureScanInfo info = JSureDataDirHub.getInstance()
				.getCurrentScanInfo();
		if (info == null) {
			return null;
		}
		Set<? extends IDropInfo> promiseWarningDrops = info
				.getDropsOfType(PromiseWarningDrop.class);
		for (IDropInfo id : promiseWarningDrops) {
			final String resource = getResource(id);
			/*
			 * We filter results based upon the resource.
			 */
			if (ModelingProblemFilterUtility.showResource(resource))
				contents.add(id);
		}
		Collections.sort(contents, sortByLocation);
		return info.getLabel();
	}

	public Image getColumnImage(Object element, int columnIndex) {
		if (columnIndex == 0) {
			if (element instanceof IDropInfo) {
				IDropInfo id = (IDropInfo) element;
				if (!id.getProposals().isEmpty()) {
					return SLImages
							.getImage(CommonImages.IMG_ANNOTATION_ERROR_PROPOSED);
				}
			}
			return SLImages.getImage(CommonImages.IMG_ANNOTATION_ERROR);
		} else {
			return null;
		}
	}
}