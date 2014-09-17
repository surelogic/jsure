package com.surelogic.jsure.client.eclipse.model.java;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.eclipse.swt.graphics.Image;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.jsure.client.eclipse.views.JSureDecoratedImageUtility;
import com.surelogic.jsure.client.eclipse.views.JSureDecoratedImageUtility.Flag;

public abstract class ElementWithChildren extends Element {

  protected ElementWithChildren(@Nullable Element parent) {
    super(parent);
  }

  @NonNull
  private final ArrayList<Element> f_children = new ArrayList<Element>();

  @Override
  final void addChild(Element child) {
    if (child == null)
      return;
    f_children.add(child);
  }

  @Override
  @NonNull
  public final Element[] getChildren() {
    return f_children.toArray(new Element[f_children.size()]);
  }

  @NonNull
  final List<Element> getChildrenAsListReference() {
    return f_children;
  }

  /**
   * Set via {@link #updateFlagsDeep(Element)}
   */
  final EnumSet<Flag> f_descendantDecoratorFlags = EnumSet.noneOf(Flag.class);

  @Override
  @Nullable
  public final Image getImage() {
    final Image baseImage = getElementImage();
    if (baseImage == null)
      return null;
    final EnumSet<Flag> flags = f_descendantDecoratorFlags;
    if (f_highlightDifferences) {
      if (descendantHasDifference())
        flags.add(Flag.DELTA);
    } else {
      flags.remove(Flag.DELTA);
    }
    return JSureDecoratedImageUtility.getImage(baseImage, flags);
  }
}
