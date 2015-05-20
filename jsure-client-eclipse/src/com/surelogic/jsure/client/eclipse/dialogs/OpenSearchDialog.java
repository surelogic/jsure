package com.surelogic.jsure.client.eclipse.dialogs;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;

import com.surelogic.common.CommonImages;
import com.surelogic.common.ui.SLImages;
import com.surelogic.jsure.client.eclipse.model.selection.Selection;

public final class OpenSearchDialog extends AbstractSearchDialog {
  Selection f_result = null;

  /**
   * Returns the selection chosen by the user, or <code>null</code> if nothing
   * was selected.
   * 
   * @return the selection chosen by the user, or <code>null</code> if nothing
   *         was selected.
   */
  public Selection getSelection() {
    return f_result; // my be null
  }

  public OpenSearchDialog(Shell parentShell) {
    super(parentShell);
  }

  @Override
  protected Control createDialogArea(Composite parent) {
    return setupDialogArea(parent, "Select a search to open", SWT.FULL_SELECTION);
  }

  @Override
  protected void createMediator(final Table projectList) {
    f_mediator = new Mediator(projectList) {
      @Override
      void okPressed() {
        if (f_searchTable.getSelectionCount() > 0) {
          f_result = f_manager.getSavedSelection(f_searchTable.getSelection()[0].getText());
        }
      }
    };
  }

  @Override
  protected void configureShell(Shell newShell) {
    super.configureShell(newShell);
    newShell.setText("Open Search");
    newShell.setImage(SLImages.getImage(CommonImages.IMG_JSURE_FINDER));
  }
}
