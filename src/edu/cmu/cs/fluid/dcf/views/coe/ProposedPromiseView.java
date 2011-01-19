package edu.cmu.cs.fluid.dcf.views.coe;

import java.util.*;

import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;

import com.surelogic.common.i18n.I18N;
import com.surelogic.jsure.client.eclipse.refactor.ProposedPromisesRefactoringAction;

import edu.cmu.cs.fluid.dcf.views.*;
import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.sea.*;

public class ProposedPromiseView extends AbstractResultsTableView<IProposedPromiseDropInfo> {
	private final Action f_annotate = new ProposedPromisesRefactoringAction() {
		@Override
		protected List<? extends IProposedPromiseDropInfo> getProposedDrops() {
			return getSelectedRows();
		}

		@Override
		protected String getDialogTitle() {
			return I18N.msg("jsure.eclipse.proposed.promises.edit");
		}
	};

	public ProposedPromiseView() {
		super(SWT.MULTI, IProposedPromiseDropInfo.class, new ProposedPromiseContentProvider());
	}

	@Override
	protected void makeActions() {
		f_annotate.setText(I18N.msg("jsure.eclipse.proposed.promises.edit"));
		f_annotate.setToolTipText(I18N
				.msg("jsure.eclipse.proposed.promises.tip"));
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
				}
			}
		}
	}
}
