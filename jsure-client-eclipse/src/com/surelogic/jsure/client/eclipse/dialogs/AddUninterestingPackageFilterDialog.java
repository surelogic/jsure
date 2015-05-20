package com.surelogic.jsure.client.eclipse.dialogs;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.surelogic.common.CommonImages;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.ui.EclipseUIUtility;
import com.surelogic.common.ui.SLImages;

public final class AddUninterestingPackageFilterDialog extends TitleAreaDialog {

  private static final int CONTENTS_WIDTH_HINT = 400;

  private static enum State {
    EMPTY, OK, DUPLICATE, BAD
  };

  private final List<String> f_existingFilters;
  private Text f_filterText;

  public AddUninterestingPackageFilterDialog(List<String> existingFilters) {
    this(EclipseUIUtility.getShell(), existingFilters);
  }

  public AddUninterestingPackageFilterDialog(Shell parentShell, List<String> existingFilters) {
    super(parentShell);
    if (existingFilters == null)
      existingFilters = Collections.emptyList();
    f_existingFilters = existingFilters;
  }

  private String f_resultFilter = null;

  /**
   * Gets the filter entered by the user into this dialog. The filter should be
   * a valid Java regular expression and should not be a duplicate of an
   * existing filter. The value will be {@code null} if the dialog was
   * cancelled.
   * 
   * @return the filter entered by the user or {@code null} if none.
   */
  public String getFilter() {
    return f_resultFilter;
  }

  @Override
  protected final void configureShell(Shell newShell) {
    super.configureShell(newShell);
    newShell.setImage(SLImages.getImage(CommonImages.IMG_FILTER));
    newShell.setText(I18N.msg("jsure.dialog.uninteresting.filter.title"));
  }

  @Override
  protected Control createContents(Composite parent) {
    Control contents = super.createContents(parent);
    setTitle(I18N.msg("jsure.dialog.uninteresting.filter.subtitle"));
    return contents;
  }

  @Override
  protected Control createDialogArea(Composite parent) {
    Composite composite = (Composite) super.createDialogArea(parent);

    final Composite panel = new Composite(composite, SWT.NONE);
    panel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    final GridLayout layout = new GridLayout(2, false);
    layout.verticalSpacing = 20;
    panel.setLayout(layout);

    final Label directions = new Label(panel, SWT.WRAP);
    GridData data = new GridData(SWT.FILL, SWT.NONE, true, false, 2, 1);
    data.widthHint = CONTENTS_WIDTH_HINT;
    directions.setLayoutData(data);
    directions.setText(I18N.msg("jsure.dialog.uninteresting.filter.directions"));

    final Label variableLabel = new Label(panel, SWT.NONE);
    variableLabel.setText("Filter:");
    variableLabel.setForeground(getShell().getDisplay().getSystemColor(SWT.COLOR_BLUE));
    variableLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
    f_filterText = new Text(panel, SWT.SINGLE);
    f_filterText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    f_filterText.setText("");
    f_filterText.addModifyListener(new ModifyListener() {
      @Override
      public void modifyText(ModifyEvent e) {
        checkIfFilterisOkay();
      }
    });
    return composite;
  }

  @Override
  protected void createButtonsForButtonBar(Composite parent) {
    super.createButtonsForButtonBar(parent);
    checkIfFilterisOkay();
  }

  void checkIfFilterisOkay() {
    if (f_filterText == null || f_filterText.isDisposed())
      return;

    State state = State.OK; // assume the best

    final String pFilter = f_filterText.getText();
    if (pFilter == null || "".equals(pFilter)) {
      state = State.EMPTY;
    } else if (f_existingFilters.contains(pFilter)) {
      state = State.DUPLICATE;
    } else {
      try {
        Pattern.compile(pFilter);
      } catch (PatternSyntaxException ignore) {
        state = State.BAD;
      }
    }

    switch (state) {
    case EMPTY:
      setMessage(I18N.msg("jsure.dialog.uninteresting.filter.empty"), IMessageProvider.INFORMATION);
      break;
    case OK:
      setMessage(I18N.msg("jsure.dialog.uninteresting.filter.ok"), IMessageProvider.INFORMATION);
      break;
    case DUPLICATE:
      setMessage(I18N.msg("jsure.dialog.uninteresting.filter.dup"), IMessageProvider.WARNING);
      break;
    case BAD:
      setMessage(I18N.msg("jsure.dialog.uninteresting.filter.bad"), IMessageProvider.ERROR);
      break;
    }

    final Button ok = getButton(IDialogConstants.OK_ID);
    if (ok != null && !ok.isDisposed())
      ok.setEnabled(state == State.OK);
  }

  @Override
  protected void okPressed() {
    f_resultFilter = f_filterText.getText();
    super.okPressed();
  }
}
