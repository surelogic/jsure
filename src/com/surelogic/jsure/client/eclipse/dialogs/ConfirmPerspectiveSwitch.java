package com.surelogic.jsure.client.eclipse.dialogs;

import org.eclipse.swt.widgets.Shell;

import com.surelogic.common.CommonImages;
import com.surelogic.common.ui.dialogs.AbstractConfirmPerspectiveSwitch;
import com.surelogic.jsure.client.eclipse.preferences.PreferenceConstants;
import com.surelogic.jsure.perspectives.CodeVerificationPerspective;

public final class ConfirmPerspectiveSwitch extends
		AbstractConfirmPerspectiveSwitch {

	public static final ConfirmPerspectiveSwitch prototype = new ConfirmPerspectiveSwitch();

	private ConfirmPerspectiveSwitch() {
		super(CodeVerificationPerspective.class.getName(),
				PreferenceConstants.prototype);
	}

	@Override
	protected String getLogo() {
		return CommonImages.IMG_JSURE_LOGO;
	}

	@Override
	protected String getShortPrefix() {
		return "jsure.";
	}

	/**
	 * Checks if the Code Review perspective should be opened.
	 * 
	 * @param shell
	 *            a shell.
	 * @return {@code true} if the Code Review perspective should be opened,
	 *         {@code false} otherwise.
	 */
	public static boolean toCodeVerification(Shell shell) {
		return prototype.toPerspective(shell);
	}
}
