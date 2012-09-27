package com.surelogic.jsure.client.eclipse.views.verification;

import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

import com.surelogic.Utility;
import com.surelogic.common.CommonImages;
import com.surelogic.jsure.client.eclipse.views.ResultsImageDescriptor;

@Utility
public final class ColumnLabelProviderUtility {

  static final StyledCellLabelProvider TREE = new StyledCellLabelProvider() {

    private Color f_onClauseColor;

    private Color getOnClauseColor() {
      if (f_onClauseColor == null) {
        f_onClauseColor = new Color(Display.getCurrent(), 149, 125, 71);
        Display.getCurrent().disposeExec(new Runnable() {
          public void run() {
            f_onClauseColor.dispose();
          }
        });
      }
      return f_onClauseColor;
    }

    @Override
    public void update(ViewerCell cell) {
      if (cell.getElement() instanceof Element) {
        final Element element = (Element) cell.getElement();
        final boolean duplicate = hasAncestorWithSameDrop(element);
        final String label;
        if (duplicate) {
          label = "\u2191  " + element.getLabel();
        } else {
          label = element.getLabel();
        }
        cell.setText(label);
        cell.setImage(element.getImage());

        if (element instanceof ElementPromiseDrop) {
          int index = label.indexOf(" on ");
          if (index != -1) {
            StyleRange[] ranges = { new StyleRange(index, label.length() - index, getOnClauseColor(), null) };
            cell.setStyleRanges(ranges);
          }
        }
      } else
        super.update(cell);
    }

    private boolean hasAncestorWithSameDrop(Element element) {
      if (element instanceof ElementDrop)
        if (((ElementDrop) element).getAncestorWithSameDropOrNull() != null)
          return true;
      return false;
    }
  };

  static abstract class VerificationStatusCellLabelProvider extends CellLabelProvider {
    boolean isNotEmptyOrNull(String value) {
      if (value == null)
        return false;
      if ("".equals(value))
        return false;
      return true;
    }
  }

  static final CellLabelProvider PROJECT = new VerificationStatusCellLabelProvider() {

    private final ResultsImageDescriptor f_projectRid = new ResultsImageDescriptor(CommonImages.IMG_PROJECT, 0,
        VerificationStatusView.ICONSIZE);

    @Override
    public void update(ViewerCell cell) {
      if (cell.getElement() instanceof Element) {
        final Element element = (Element) cell.getElement();
        final String project = element.getProjectNameOrNull();
        if (isNotEmptyOrNull(project)) {
          cell.setText(project);
          cell.setImage(f_projectRid.getCachedImage());
        }
      }
    }
  };

  static final CellLabelProvider PACKAGE = new VerificationStatusCellLabelProvider() {

    private final ResultsImageDescriptor f_packageRid = new ResultsImageDescriptor(CommonImages.IMG_PACKAGE, 0,
        VerificationStatusView.ICONSIZE);

    @Override
    public void update(ViewerCell cell) {
      if (cell.getElement() instanceof Element) {
        final Element element = (Element) cell.getElement();
        final String pkg = element.getPackageNameOrNull();
        if (isNotEmptyOrNull(pkg)) {
          cell.setText(pkg);
          cell.setImage(f_packageRid.getCachedImage());
        }
      }
    }
  };

  static final CellLabelProvider TYPE = new VerificationStatusCellLabelProvider() {

    private final ResultsImageDescriptor f_classRid = new ResultsImageDescriptor(CommonImages.IMG_CLASS, 0,
        VerificationStatusView.ICONSIZE);
    private final ResultsImageDescriptor f_interfaceRid = new ResultsImageDescriptor(CommonImages.IMG_INTERFACE, 0,
        VerificationStatusView.ICONSIZE);
    private final ResultsImageDescriptor f_enumRid = new ResultsImageDescriptor(CommonImages.IMG_ENUM, 0,
        VerificationStatusView.ICONSIZE);

    @Override
    public void update(ViewerCell cell) {
      if (cell.getElement() instanceof Element) {
        final Element element = (Element) cell.getElement();
        final String typeName = element.getSimpleTypeNameOrNull();
        if (isNotEmptyOrNull(typeName)) {
          cell.setText(typeName);
          cell.setImage(f_classRid.getCachedImage());
        }
      }
    }
  };

  static final CellLabelProvider LINE = new CellLabelProvider() {

    @Override
    public void update(ViewerCell cell) {
      if (cell.getElement() instanceof Element) {
        final Element element = (Element) cell.getElement();
        final String line = element.getLineNumberAsStringOrNull();
        if (line != null)
          cell.setText(line);
      }
    }
  };
}
