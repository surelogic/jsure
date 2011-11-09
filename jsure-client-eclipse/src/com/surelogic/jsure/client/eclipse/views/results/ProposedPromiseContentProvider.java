package com.surelogic.jsure.client.eclipse.views.results;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.swt.graphics.Image;

import com.surelogic.common.CommonImages;
import com.surelogic.common.ui.SLImages;
import com.surelogic.jsure.core.scans.JSureDataDirHub;
import com.surelogic.jsure.core.scans.JSureScanInfo;

import edu.cmu.cs.fluid.sea.IProposedPromiseDropInfo;
import edu.cmu.cs.fluid.sea.ProposedPromiseDrop;

public final class ProposedPromiseContentProvider extends
		AbstractResultsTableContentProvider<IProposedPromiseDropInfo> {
	ProposedPromiseContentProvider() {
		super("Proposed Promise");
	}

	protected String getAndSortResults(List<IProposedPromiseDropInfo> contents) {
		final JSureScanInfo info = JSureDataDirHub.getInstance()
				.getCurrentScanInfo();
		if (info == null) {
			return null;
		}
		List<IProposedPromiseDropInfo> proposedPromiseDrops = ProposedPromiseDrop
				.filterOutDuplicates(info
						.<IProposedPromiseDropInfo, ProposedPromiseDrop> getDropsOfType(ProposedPromiseDrop.class));
		for (IProposedPromiseDropInfo id : proposedPromiseDrops) {
			if (id != null && id.getSrcRef() != null) {
				// TODO omit annotations on implicitly created methods in enums?
				/*
				 * if (id.getSrcRef() == null) {
				 * System.out.println("Got proposal on "
				 * +DebugUnparser.toString(id.getNode())+" in "+
				 * JavaNames.getFullTypeName
				 * (VisitUtil.getEnclosingType(id.getNode()))); }
				 */
				contents.add(id);
			}
		}
		Collections.sort(contents, sortByProposal);
		return info.getLabel();
	}

	private final Comparator<IProposedPromiseDropInfo> sortByProposal = new Comparator<IProposedPromiseDropInfo>() {
		public int compare(IProposedPromiseDropInfo d1, IProposedPromiseDropInfo d2) {
			return d1.getJavaAnnotation().compareTo(d2.getJavaAnnotation());
		}
	};
	
	public Image getColumnImage(Object element, int columnIndex) {
		if (columnIndex == 0) {
			return SLImages.getImage(CommonImages.IMG_ANNOTATION_PROPOSED);
		} else {
			return null;
		}
	}

	@Override
	protected String getMainColumnText(IProposedPromiseDropInfo d) {
		return d.getJavaAnnotation().substring(1);
	}
}