package com.surelogic.jsure.client.eclipse.views.metrics.sloc;

import org.eclipse.swt.graphics.Image;

import com.surelogic.common.CommonImages;
import com.surelogic.common.ui.JDTUIUtility;
import com.surelogic.common.ui.SLImages;

/**
 * Represents SLOC metric information for a Java compilation unit.
 */
public final class SlocElementLeaf extends SlocElement {

  SlocElementLeaf(SlocElement parent, String label, int blankLineCount, int containsCommentLineCount, int javaDeclarationCount,
      int javaStatementCount, int lineCount, int semicolonCount) {
    super(parent, label);
    f_blankLineCount = blankLineCount;
    f_containsCommentLineCount = containsCommentLineCount;
    f_javaDeclarationCount = javaDeclarationCount;
    f_javaStatementCount = javaStatementCount;
    f_lineCount = lineCount;
    f_semicolonCount = semicolonCount;
  }

  @Override
  public SlocElement[] getChildren() {
    return SlocElement.EMPTY;
  }

  @Override
  public boolean highlightDueToSlocThreshold(SlocOptions options) {
    final long threshold = options.getThreshold();
    final boolean showAbove = options.getThresholdShowAbove();
    final long metricValue;
    switch (options.getSelectedColumnTitleIndex()) {
    case 1: // Blank Lines
      metricValue = f_blankLineCount;
      break;
    case 2: // Commented Lines
      metricValue = f_containsCommentLineCount;
      break;
    case 3: // Java Declarations
      metricValue = f_javaDeclarationCount;
      break;
    case 4: // Java Statements
      metricValue = f_javaStatementCount;
      break;
    case 5: // Semicolon Count
      metricValue = f_semicolonCount;
      break;
    default: // SLOC (0 and default)
      metricValue = f_lineCount;
      break;
    }
    return showAbove ? metricValue >= threshold : metricValue <= threshold;
  }

  @Override
  public Image getImage() {
    return SLImages.getImage(CommonImages.IMG_JAVA_COMP_UNIT);
  }

  public void tryToOpenInJavaEditor() {
    if (getParent() == null || getParent().getParent() == null)
      return; // can't figure out what to open
    /*
     * This method makes a lot of assumptions about the tree. First the leaf is
     * of the form "Foo.java" which is changed to "Foo" and assumed to be a type
     * name. Second, the parent node is a package name. Third the parent node of
     * the parent node is a project name.
     */
    String cu = getLabel();
    cu = cu.substring(0, cu.length() - 5); // take off ".java"
    String pkg = getParent().getLabel(); // Java package name
    String proj = getParent().getParent().getLabel(); // project name
    JDTUIUtility.tryToOpenInEditor(proj, pkg, cu);
  }
}
