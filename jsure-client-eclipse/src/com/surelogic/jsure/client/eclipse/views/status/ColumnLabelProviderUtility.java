package com.surelogic.jsure.client.eclipse.views.status;

import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Image;

import com.surelogic.Utility;
import com.surelogic.common.CommonImages;
import com.surelogic.common.SLUtility;
import com.surelogic.common.ui.SLImages;
import com.surelogic.dropsea.IDrop;
import com.surelogic.dropsea.IResultFolderDrop;
import com.surelogic.dropsea.ScanDifferences;
import com.surelogic.jsure.client.eclipse.JSureClientUtility;
import com.surelogic.jsure.client.eclipse.views.JSureDecoratedImageUtility;

@Utility
public final class ColumnLabelProviderUtility {

  private static void highlightRowIfNewOrDiff(ViewerCell cell) {
    if (Element.f_highlightDifferences) {
      if (cell.getElement() instanceof ElementDrop) {
        final ElementDrop element = (ElementDrop) cell.getElement();
        if (!element.isSame())
          cell.setBackground(JSureClientUtility.getDiffHighlightColorNewChanged());
        return;
      }
    }
    cell.setBackground(null);
  }

  static final StyledCellLabelProvider TREE = new StyledCellLabelProvider() {

    @Override
    public void update(ViewerCell cell) {
      highlightRowIfNewOrDiff(cell);

      if (cell.getElement() instanceof Element) {
        final Element element = (Element) cell.getElement();
        final boolean duplicate = hasAncestorWithSameDrop(element);
        String label = element.getLabel();
        if (duplicate)
          label = "\u2191  " + label;

        if (element instanceof ElementAnalysisResultDrop) {
          final Element parent = element.getParent();
          final boolean parentIsAnOrFolder = parent instanceof ElementResultFolderDrop
              && ((ElementResultFolderDrop) parent).getDrop().getLogicOperator() == IResultFolderDrop.LogicOperator.OR;
          if (parentIsAnOrFolder) {
            final String OR = "(or)  ";
            label = OR + label;
            StyleRange[] ranges = { new StyleRange(0, OR.length(), JSureClientUtility.getSubtleTextColor(), null) };
            cell.setStyleRanges(ranges);
          }
        }
        cell.setText(label);
        cell.setImage(element.getImage());

        if (element instanceof ElementPromiseDrop) {
          int index = label.indexOf(" on ");
          if (index != -1) {
            StyleRange[] ranges = { new StyleRange(index, label.length() - index, JSureClientUtility.getSubtleTextColor(), null) };
            cell.setStyleRanges(ranges);
          }
        }
        if (element instanceof ElementCategory) {
          if (label.endsWith(")")) {
            int start = label.lastIndexOf('(');
            if (start != -1) {
              StyleRange[] ranges = { new StyleRange(start, label.length() - start, JSureClientUtility.getSubtleTextColor(), null) };
              cell.setStyleRanges(ranges);
            }
          }
        }
        if (element instanceof ElementProposedPromiseDrop) {
          final String prefixEnd = "promise)";
          int index = label.indexOf(prefixEnd);
          if (index != -1) {
            StyleRange[] ranges = { new StyleRange(0, index + prefixEnd.length(), JSureClientUtility.getSubtleTextColor(), null) };
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

  static final CellLabelProvider PROJECT = new CellLabelProvider() {

    @Override
    public void update(ViewerCell cell) {
      highlightRowIfNewOrDiff(cell);

      if (cell.getElement() instanceof Element) {
        final Element element = (Element) cell.getElement();
        cell.setText(element.getProjectNameOrNull());
        cell.setImage(element.getProjectImageOrNull());
      }
    }
  };

  static final CellLabelProvider PACKAGE = new CellLabelProvider() {

    private final Image f_packageImage = JSureDecoratedImageUtility.getImage(CommonImages.IMG_PACKAGE);

    @Override
    public void update(ViewerCell cell) {
      highlightRowIfNewOrDiff(cell);

      if (cell.getElement() instanceof Element) {
        final Element element = (Element) cell.getElement();
        final String pkg = element.getPackageNameOrNull();
        if (SLUtility.isNotEmptyOrNull(pkg)) {
          cell.setText(pkg);
          cell.setImage(f_packageImage);
        }
      }
    }
  };

  static final CellLabelProvider TYPE = new CellLabelProvider() {

    @Override
    public void update(ViewerCell cell) {
      highlightRowIfNewOrDiff(cell);

      if (cell.getElement() instanceof ElementDrop) {
        final ElementDrop element = (ElementDrop) cell.getElement();
        final String typeName = element.getSimpleTypeNameOrNull();
        if (SLUtility.isNotEmptyOrNull(typeName)) {
          cell.setText(typeName);
          cell.setImage(element.getSimpleTypeImageOrNull());
        }
      }
    }
  };

  static final CellLabelProvider LINE = new CellLabelProvider() {

    @Override
    public void update(ViewerCell cell) {
      highlightRowIfNewOrDiff(cell);

      if (cell.getElement() instanceof Element) {
        final Element element = (Element) cell.getElement();
        final String line = element.getLineNumberAsStringOrNull();
        if (line != null)
          cell.setText(line);
      }
    }
  };

  static final CellLabelProvider DIFF = new CellLabelProvider() {

    @Override
    public void update(ViewerCell cell) {
      highlightRowIfNewOrDiff(cell);

      final ScanDifferences diff = Element.f_diff;
      if (diff != null && cell.getElement() instanceof ElementDrop) {
        final ElementDrop element = (ElementDrop) cell.getElement();
        String cellText = null;
        Image cellImage = null;
        if (element.isNew()) {
          cellImage = SLImages.getGrayscaleImage(element.getElementImage());
          cellText = "New";
        } else {
          final IDrop oldDrop = element.getChangedFromDropOrNull();
          if (oldDrop != null) {
            cellImage = SLImages.getGrayscaleImage(element.getElementImageChangedFromDropOrNull());
            cellText = "Changed";
            final String whatChanged = ScanDifferences.getMessageAboutWhatChanged(element.getChangedFromDropOrNull(),
                element.getDrop());
            if (whatChanged != null) {
              cellText += " to " + whatChanged;
            }
          }
        }
        if (cellImage != null) {
          cell.setImage(cellImage);
        }
        if (cellText != null) {
          cell.setText(cellText);
          cell.setForeground(JSureClientUtility.getSubtleTextColor());
        }
      }
    }
  };
}
