package com.surelogic.jsure.client.eclipse.actions;

import org.eclipse.jface.action.IAction;

import com.surelogic.common.CommonImages;
import com.surelogic.common.core.EclipseUtility;
import com.surelogic.common.license.SLLicenseProduct;
import com.surelogic.common.ui.actions.AbstractMainAction;
import com.surelogic.common.ui.serviceability.SendServiceMessageWizard;
import com.surelogic.jsure.client.eclipse.Activator;

public final class SendTipAction extends AbstractMainAction {
	public void run(IAction action) {
		SendServiceMessageWizard.openTip(SLLicenseProduct.JSURE + " "
				+ EclipseUtility.getVersion(Activator.getDefault()),
				CommonImages.IMG_JSURE_LOGO);
	}
}
