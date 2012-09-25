package com.surelogic.jsure.client.eclipse.views.verification;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.graphics.Image;

import com.surelogic.NonNull;

public abstract class AbstractElementColumnLabelProvider extends ColumnLabelProvider {

  @Override
  public Image getImage(Object element) {
    if (element instanceof Element)
      return getImageFromElement((Element) element);
    else
      return super.getImage(element);
  }

  abstract Image getImageFromElement(@NonNull Element element);

  @Override
  public String getText(Object element) {
    if (element instanceof Element)
      return getTextFromElement((Element) element);
    else
      return super.getText(element);
  }

  abstract String getTextFromElement(@NonNull Element element);

  protected boolean isNotEmptyOrNull(String value) {
    if (value == null)
      return false;
    if ("".equals(value))
      return false;
    return true;
  }
}
