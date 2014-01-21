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
  public boolean aboveSlocThreshold(int slocThreshold) {
    return f_lineCount >= slocThreshold;
  }

  @Override
  public Image getImage() {
    return SLImages.getImage(CommonImages.IMG_JAVA_COMP_UNIT);
  }

  public void tryToOpenInJavaEditor() {
    /*
     * This method makes a lot of assumptions about the tree. First the leaf is
     * of the form "Foo.java" which is changed to "Foo" and assumed to be a type
     * name. Second, the parent node is a package name. Third the parent node of
     * the parent node is a project name.
     */
    String cu = getLabel();
    cu = cu.substring(0, cu.length() - 5);
    String pkg = getParent().getLabel();
    String proj = getParent().getParent().getLabel();
    JDTUIUtility.tryToOpenInEditor(proj, pkg, cu);
  }
}
