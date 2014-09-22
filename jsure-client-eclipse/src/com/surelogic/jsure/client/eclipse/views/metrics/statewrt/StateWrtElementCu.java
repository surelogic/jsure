package com.surelogic.jsure.client.eclipse.views.metrics.statewrt;

import org.eclipse.swt.graphics.Image;

import com.surelogic.Nullable;
import com.surelogic.common.CommonImages;
import com.surelogic.common.ui.SLImages;
import com.surelogic.jsure.client.eclipse.Activator;

public final class StateWrtElementCu extends StateWrtElement {

  protected StateWrtElementCu(StateWrtElementPackage parent, @Nullable String javaFileName) {
    super(parent, javaFileName);
  }

  @Override
  public Image getImage() {
    return SLImages.getImage(CommonImages.IMG_JAVA_COMP_UNIT);
  }

  @Override
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
    System.out.println("tryToOpenInEditor(" + proj + ", " + pkg + ", " + cu);
    Activator.tryToOpenInEditor(proj, pkg, cu);
  }
}
