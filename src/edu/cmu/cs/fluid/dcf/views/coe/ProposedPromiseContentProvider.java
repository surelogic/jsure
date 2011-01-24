package edu.cmu.cs.fluid.dcf.views.coe;

import java.util.*;

import org.eclipse.swt.graphics.Image;

import com.surelogic.common.CommonImages;
import com.surelogic.common.eclipse.SLImages;

import edu.cmu.cs.fluid.sea.*;

public final class ProposedPromiseContentProvider extends AbstractResultsTableContentProvider<IProposedPromiseDropInfo> {
	ProposedPromiseContentProvider() {
		super("Proposed Promise");
	}
	
	protected void getAndSortResults(List<IProposedPromiseDropInfo> contents) {
		List<IProposedPromiseDropInfo> proposedPromiseDrops = 
			ProposedPromiseDrop.filterOutDuplicates(
					PersistentDropInfo.getInstance().
					<IProposedPromiseDropInfo,ProposedPromiseDrop>getDropsOfType(ProposedPromiseDrop.class));
		for (IProposedPromiseDropInfo id : proposedPromiseDrops) {
			if (id != null && id.getSrcRef() != null) {
				// TODO omit annotations on implicitly created methods in enums?
				/*
				if (id.getSrcRef() == null) {
					System.out.println("Got proposal on "+DebugUnparser.toString(id.getNode())+" in "+
							JavaNames.getFullTypeName(VisitUtil.getEnclosingType(id.getNode())));
				}
				*/
				contents.add(id);
			}
		}
		Collections.sort(contents, sortAsString);
	}

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