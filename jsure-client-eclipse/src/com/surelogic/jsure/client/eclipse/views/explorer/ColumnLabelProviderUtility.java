package com.surelogic.jsure.client.eclipse.views.explorer;

import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.custom.StyleRange;

import com.surelogic.Utility;
import com.surelogic.dropsea.ScanDifferences;
import com.surelogic.jsure.client.eclipse.JSureClientUtility;

@Utility
public final class ColumnLabelProviderUtility {

  private static void highlightRowIfNewOrDiff(ViewerCell cell) {
    // if (Element.f_highlightDifferences) {
    // if (cell.getElement() instanceof ElementDrop) {
    // final ElementDrop element = (ElementDrop) cell.getElement();
    // if (!element.isSame())
    // cell.setBackground(getDiffColor());
    // return;
    // }
    // }
    // cell.setBackground(null);
  }

  static final StyledCellLabelProvider TREE = new StyledCellLabelProvider() {

    @Override
    public void update(ViewerCell cell) {
      highlightRowIfNewOrDiff(cell);

      if (cell.getElement() instanceof Element) {
        final Element element = (Element) cell.getElement();
        String label = element.getLabel();

        if (element instanceof ElementJavaDecl) {
          /*
           * Match Eclipse with uses subtle text color for ": Object" (types)
           */
          final int colonIndex = label.indexOf(':');
          if (colonIndex != -1) {
            StyleRange[] ranges = { new StyleRange(colonIndex, label.length(), JSureClientUtility.getSubtleTextColor(), null) };
            cell.setStyleRanges(ranges);
          }
        }

        cell.setText(label);
        cell.setImage(element.getImage());

      } else
        super.update(cell);
    }
  };

  static final CellLabelProvider DIFF = new CellLabelProvider() {

    @Override
    public void update(ViewerCell cell) {
      highlightRowIfNewOrDiff(cell);

      final ScanDifferences diff = Element.f_diff;
      // if (diff != null && cell.getElement() instanceof ElementDrop) {
      // final ElementDrop element = (ElementDrop) cell.getElement();
      // String cellText = null;
      // Image cellImage = null;
      // if (element.isNew()) {
      // cellImage = element.getImageHelper(element.getImageName(),
      // element.getImageFlags(), true, false, false);
      // cellText = "New";
      // } else {
      // final IDrop oldDrop = element.getChangedFromDropOrNull();
      // if (oldDrop != null) {
      // cellImage =
      // element.getImageHelper(element.getImageNameForChangedFromDrop(),
      // element.getImageFlagsForChangedFromDrop(),
      // true, false, false);
      // cellText = "Changed";
      // final String whatChanged = element.getMessageAboutWhatChangedOrNull();
      // if (whatChanged != null) {
      // cellText += " to " + whatChanged;
      // }
      // }
      // }
      // if (cellImage != null) {
      // cell.setImage(cellImage);
      // }
      // if (cellText != null) {
      // cell.setText(cellText);
      // cell.setForeground(JSureClientUtility.getSubtileTextColor());
      // }
      // }
    }
  };
}
