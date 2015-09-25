package com.surelogic.jsure.client.eclipse.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import com.surelogic.common.CommonImages;
import com.surelogic.common.core.EclipseUtility;
import com.surelogic.common.license.SLLicenseProduct;
import com.surelogic.common.ui.serviceability.SendServiceMessageWizard;

public class SendProblemHandler extends AbstractHandler {

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    SendServiceMessageWizard.openProblemReport(SLLicenseProduct.JSURE + " " + EclipseUtility.getSureLogicToolsVersion(),
        CommonImages.IMG_JSURE_LOGO);
    return null;
  }
}
