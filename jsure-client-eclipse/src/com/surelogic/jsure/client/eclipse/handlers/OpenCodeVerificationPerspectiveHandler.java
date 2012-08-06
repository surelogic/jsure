package com.surelogic.jsure.client.eclipse.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import com.surelogic.common.ui.EclipseUIUtility;
import com.surelogic.jsure.client.eclipse.perspectives.CodeVerificationPerspective;

public final class OpenCodeVerificationPerspectiveHandler extends
		AbstractHandler {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		EclipseUIUtility.showPerspective(CodeVerificationPerspective.class
				.getName());
		return null;
	}
}
