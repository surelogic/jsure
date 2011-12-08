package com.surelogic.jsure.client.eclipse.views.results;

import java.util.*;

import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;

import com.surelogic.common.CommonImages;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.ui.SLImages;
import com.surelogic.jsure.client.eclipse.views.*;

import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.sea.*;

public class ProposedPromiseView extends AbstractScanPagedTableTreeView<IProposedPromiseDropInfo> {
	private static final String ONLY_ANNO_DERIVED = "Show only proposals derived from annotations";
	
	public ProposedPromiseView() {
		super(SWT.MULTI, IProposedPromiseDropInfo.class, new ProposedPromiseContentProvider(true));
	}

	private final Action f_toggleFilter = new Action(ONLY_ANNO_DERIVED, IAction.AS_CHECK_BOX) {
		@Override
		public void run() {
			// TODO
		}
	};
	
	@Override
	protected void makeActions() {
		f_annotate.setText(I18N.msg("jsure.eclipse.proposed.promises.edit"));
		f_annotate.setToolTipText(I18N
				.msg("jsure.eclipse.proposed.promises.tip"));
		
		f_toggleFilter.setImageDescriptor(SLImages
				.getImageDescriptor(CommonImages.IMG_ANNOTATION_DELTA));
		f_toggleFilter.setToolTipText(ONLY_ANNO_DERIVED);
	}

	@Override
	protected void fillLocalPullDown(IMenuManager manager) {
		super.fillLocalPullDown(manager);
		manager.add(f_toggleFilter);
	}

	@Override
	protected void fillLocalToolBar(IToolBarManager manager) {
		super.fillLocalToolBar(manager);
		manager.add(f_toggleFilter);
	}
	
	@Override
	protected void fillContextMenu(final IMenuManager manager,
			final IStructuredSelection s) {
		if (!s.isEmpty()) {
			for (Object o : s.toArray()) {
				final IProposedPromiseDropInfo p = (IProposedPromiseDropInfo) o;
				ISrcRef ref = p.getSrcRef();
				if (ref != null) {
					manager.add(f_annotate);
					return; // Only needs one
				}
			}
		}
	}

	@Override
	protected List<? extends IProposedPromiseDropInfo> getSelectedProposals() {
		return getSelectedRows();
	}
}
