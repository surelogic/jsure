package com.surelogic.jsure.client.eclipse.actions;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.action.IAction;

import com.surelogic.common.CommonImages;
import com.surelogic.common.eclipse.SLImages;
import com.surelogic.common.eclipse.actions.AbstractMainAction;
import com.surelogic.jsure.client.eclipse.analysis.JavacDriver;
import com.surelogic.jsure.client.eclipse.dialogs.JavaProjectSelectionDialog;

public class RunJSureMainAction extends AbstractMainAction {

    public void run(IAction action) {
        final IJavaProject focus = JavaProjectSelectionDialog.getProject(
                "Select the project to verify:", "Run JSure", SLImages
                        .getImage(CommonImages.IMG_JSURE_VERIFY));
        if (focus != null) {
            //JavacDriver.getInstance().doBuild(focus.getProject());
        }
    }
}
