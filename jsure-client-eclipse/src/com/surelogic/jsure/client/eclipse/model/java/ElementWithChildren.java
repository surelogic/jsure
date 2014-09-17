package com.surelogic.jsure.client.eclipse.model.java;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.eclipse.swt.graphics.Image;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.dropsea.IAnalysisResultDrop;
import com.surelogic.dropsea.IDrop;
import com.surelogic.dropsea.IHintDrop;
import com.surelogic.dropsea.IProofDrop;
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

  private EnumSet<Flag> f_descendantDecoratorFlagsCache = null;
  private boolean f_descendantDeltaCache;

  final EnumSet<Flag> getDescendantDecoratorFlags() {
    if (f_descendantDecoratorFlagsCache == null) {
      f_descendantDecoratorFlagsCache = descendantDecoratorFlagsHelper(this);
      /*
       * Fix up verification proof result (+ and X are in an X proof most of the
       * time)
       */
      if (f_descendantDecoratorFlagsCache.contains(Flag.INCONSISTENT)) {
        f_descendantDecoratorFlagsCache.remove(Flag.CONSISTENT);
        f_descendantDecoratorFlagsCache.remove(Flag.UNUSED_CONSISTENT);
        f_descendantDecoratorFlagsCache.remove(Flag.UNUSED_INCONSISTENT);
      }
      if (f_descendantDecoratorFlagsCache.contains(Flag.CONSISTENT)) {
        f_descendantDecoratorFlagsCache.remove(Flag.UNUSED_CONSISTENT);
        f_descendantDecoratorFlagsCache.remove(Flag.UNUSED_INCONSISTENT);
      }
      /*
       * Remember delta flag because it can be toggled on and off without a
       * rebuild of the model.
       */
      f_descendantDeltaCache = f_descendantDecoratorFlagsCache.contains(Flag.DELTA);
    }
    return f_descendantDecoratorFlagsCache;
  }

  private EnumSet<Flag> descendantDecoratorFlagsHelper(Element e) {
    EnumSet<Flag> result = EnumSet.noneOf(Flag.class);
    if (e instanceof ElementDrop) {
      final ElementDrop ed = (ElementDrop) e;
      if (!ed.isSame())
        result.add(Flag.DELTA);

      final IDrop drop = ed.getDrop();
      if (drop instanceof IProofDrop) {
        final IProofDrop pd = (IProofDrop) drop;
        if (pd.provedConsistent())
          result.add(Flag.CONSISTENT);
        else
          result.add(Flag.INCONSISTENT);
        if (pd.proofUsesRedDot())
          result.add(Flag.REDDOT);
        if (pd instanceof IAnalysisResultDrop) {
          final IAnalysisResultDrop ard = (IAnalysisResultDrop) pd;
          if (!ard.usedByProof()) {
            result.add(ard.provedConsistent() ? Flag.UNUSED_CONSISTENT : Flag.UNUSED_INCONSISTENT);
            result.remove(Flag.CONSISTENT);
            result.remove(Flag.INCONSISTENT);
          }
        }
      } else if (drop instanceof IHintDrop) {
        final IHintDrop hd = (IHintDrop) drop;
        if (hd.getHintType() == IHintDrop.HintType.WARNING)
          result.add(Flag.HINT_WARNING);
      }

    } else {
      for (Element c : e.getChildren()) {
        result.addAll(descendantDecoratorFlagsHelper(c));
      }
    }
    return result;
  }

  @Override
  @Nullable
  public final Image getImage() {
    final Image baseImage = getElementImage();
    if (baseImage == null)
      return null;
    final EnumSet<Flag> flags = getDescendantDecoratorFlags();
    if (f_highlightDifferences) {
      if (f_descendantDeltaCache)
        flags.add(Flag.DELTA);
    } else {
      flags.remove(Flag.DELTA);
    }
    return JSureDecoratedImageUtility.getImage(baseImage, flags);
  }
}
