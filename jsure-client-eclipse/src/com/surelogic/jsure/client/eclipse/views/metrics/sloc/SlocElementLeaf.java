package com.surelogic.jsure.client.eclipse.views.metrics.sloc;

import org.eclipse.swt.graphics.Image;

import com.surelogic.common.CommonImages;
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
  public Image getImage() {
    return SLImages.getImage(CommonImages.IMG_JAVA_COMP_UNIT);
  }
}
