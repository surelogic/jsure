package com.surelogic.jsure.client.eclipse.views.explorer;

import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import com.surelogic.Utility;
import com.surelogic.common.CommonImages;
import com.surelogic.common.SLUtility;
import com.surelogic.dropsea.IDrop;
import com.surelogic.dropsea.IResultFolderDrop;
import com.surelogic.dropsea.ScanDifferences;
import com.surelogic.jsure.client.eclipse.JSureClientUtility;
import com.surelogic.jsure.client.eclipse.views.JSureDecoratedImageUtility;

@Utility
public final class ColumnLabelProviderUtility {

  private static void highlightRowIfNewOrDiff(ViewerCell cell) {
//    if (Element.f_highlightDifferences) {
//      if (cell.getElement() instanceof ElementDrop) {
//        final ElementDrop element = (ElementDrop) cell.getElement();
//        if (!element.isSame())
//          cell.setBackground(getDiffColor());
//        return;
//      }
//    }
//    cell.setBackground(null);
  }

  static final StyledCellLabelProvider TREE = new StyledCellLabelProvider() {

    @Override
    public void update(ViewerCell cell) {
      highlightRowIfNewOrDiff(cell);

      if (cell.getElement() instanceof Element) {
        final Element element = (Element) cell.getElement();
        String label = element.getLabel();

//        if (element instanceof ElementAnalysisResultDrop) {
//          final Element parent = element.getParent();
//          final boolean parentIsAnOrFolder = parent instanceof ElementResultFolderDrop
//              && ((ElementResultFolderDrop) parent).getDrop().getLogicOperator() == IResultFolderDrop.LogicOperator.OR;
//          if (parentIsAnOrFolder) {
//            final String OR = "(or)  ";
//            label = OR + label;
//            StyleRange[] ranges = { new StyleRange(0, OR.length(), JSureClientUtility.getSubtileTextColor(), null) };
//            cell.setStyleRanges(ranges);
//          }
//        }
        cell.setText(label);
        cell.setImage(element.getImage());

//        if (element instanceof ElementPromiseDrop) {
//          int index = label.indexOf(" on ");
//          if (index != -1) {
//            StyleRange[] ranges = { new StyleRange(index, label.length() - index, JSureClientUtility.getSubtileTextColor(), null) };
//            cell.setStyleRanges(ranges);
//          }
//        }
//        if (element instanceof ElementCategory) {
//          if (label.endsWith(")")) {
//            int start = label.lastIndexOf('(');
//            if (start != -1) {
//              StyleRange[] ranges = { new StyleRange(start, label.length() - start, JSureClientUtility.getSubtileTextColor(), null) };
//              cell.setStyleRanges(ranges);
//            }
//          }
//        }
//        if (element instanceof ElementProposedPromiseDrop) {
//          final String prefixEnd = "promise)";
//          int index = label.indexOf(prefixEnd);
//          if (index != -1) {
//            StyleRange[] ranges = { new StyleRange(0, index + prefixEnd.length(), JSureClientUtility.getSubtileTextColor(), null) };
//            cell.setStyleRanges(ranges);
//          }
//        }
      } else
        super.update(cell);
    }
  };

  static final CellLabelProvider DIFF = new CellLabelProvider() {

    @Override
    public void update(ViewerCell cell) {
      highlightRowIfNewOrDiff(cell);

      final ScanDifferences diff = Element.f_diff;
//      if (diff != null && cell.getElement() instanceof ElementDrop) {
//        final ElementDrop element = (ElementDrop) cell.getElement();
//        String cellText = null;
//        Image cellImage = null;
//        if (element.isNew()) {
//          cellImage = element.getImageHelper(element.getImageName(), element.getImageFlags(), true, false, false);
//          cellText = "New";
//        } else {
//          final IDrop oldDrop = element.getChangedFromDropOrNull();
//          if (oldDrop != null) {
//            cellImage = element.getImageHelper(element.getImageNameForChangedFromDrop(), element.getImageFlagsForChangedFromDrop(),
//                true, false, false);
//            cellText = "Changed";
//            final String whatChanged = element.getMessageAboutWhatChangedOrNull();
//            if (whatChanged != null) {
//              cellText += " to " + whatChanged;
//            }
//          }
//        }
//        if (cellImage != null) {
//          cell.setImage(cellImage);
//        }
//        if (cellText != null) {
//          cell.setText(cellText);
//          cell.setForeground(JSureClientUtility.getSubtileTextColor());
//        }
//      }
    }
  };
}
