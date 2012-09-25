package com.surelogic.jsure.client.eclipse.views.verification;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.common.CommonImages;
import com.surelogic.common.jsure.xml.CoE_Constants;
import com.surelogic.dropsea.IHintDrop;
import com.surelogic.dropsea.IProofDrop;

public final class ElementCategory extends Element {

  static final class Builder {

    private Element f_parent;
    private final List<IProofDrop> f_proofDrops = new ArrayList<IProofDrop>();
    boolean f_provedConsistent = true;
    boolean f_proofUsesRedDot = false;
    private final List<IHintDrop> f_hintDrops = new ArrayList<IHintDrop>();
    boolean f_anyInfoHints = false;
    boolean f_anyWarningHints = false;
    private final List<ElementCategory.Builder> f_categories = new ArrayList<ElementCategory.Builder>();
    private String f_label;
    private String f_imageName;

    Builder(Element parent) {
      f_parent = parent;
    }

    Builder() {
      this(null);
    }

    void setParent(Element parent) {
      f_parent = parent;
    }

    void addProofDrop(IProofDrop proofDrop) {
      if (proofDrop == null)
        return;
      if (proofDrop.proofUsesRedDot())
        f_proofUsesRedDot = true;
      f_provedConsistent &= proofDrop.provedConsistent();
      f_proofDrops.add(proofDrop);
    }

    void addProofDropAll(Collection<IProofDrop> drops) {
      if (drops == null)
        return;
      for (IProofDrop drop : drops)
        addProofDrop(drop);
    }

    void addHintDrop(IHintDrop hintDrop) {
      if (hintDrop == null)
        return;
      if (hintDrop.getHintType() == IHintDrop.HintType.WARNING)
        f_anyWarningHints = true;
      else
        f_anyInfoHints = true;
      f_hintDrops.add(hintDrop);
    }

    void addHintDropAll(Collection<IHintDrop> drops) {
      if (drops == null)
        return;
      for (IHintDrop drop : drops)
        addHintDrop(drop);
    }

    void addCategory(ElementCategory.Builder builder) {
      if (builder == null)
        return;
      f_categories.add(builder);
    }

    void setLabel(String value) {
      f_label = value;
    }

    void setImageName(String value) {
      f_imageName = value;
    }

    boolean isValidToBuild() {
      return !f_proofDrops.isEmpty() || !f_hintDrops.isEmpty() || !f_categories.isEmpty();
    }

    ElementCategory build() {
      if (f_label == null)
        f_label = "NO LABEL";
      if (f_imageName == null)
        f_imageName = CommonImages.IMG_FOLDER;
      if (!isValidToBuild())
        throw new IllegalStateException("A category element must contain at least one element");

      /*
       * Determine necessary image flags
       */
      int flags = 0;
      if (!f_proofDrops.isEmpty()) {
        flags |= f_provedConsistent ? CoE_Constants.CONSISTENT : CoE_Constants.INCONSISTENT;
        if (f_proofUsesRedDot)
          flags |= CoE_Constants.REDDOT;
      }
      if (f_anyInfoHints && !f_anyWarningHints) {
        flags |= CoE_Constants.HINT_INFO;
      }
      if (f_anyWarningHints) {
        flags |= CoE_Constants.HINT_WARNING;
      }
      ElementCategory result = new ElementCategory(f_parent, f_label, flags, f_imageName);
      List<Element> children = new ArrayList<Element>();
      for (IProofDrop pd : f_proofDrops) {
        children.add(ElementProofDrop.factory(result, pd));
      }
      for (IHintDrop hd : f_hintDrops) {
        children.add(new ElementHintDrop(result, hd));
      }
      for (ElementCategory.Builder builder : f_categories) {
        builder.setParent(result);
        children.add(builder.build());
      }
      Collections.sort(children);
      result.f_children = children.toArray(new Element[children.size()]);
      return result;
    }
  }

  private Element[] f_children;
  private final String f_label;
  private final int f_imageFlags;
  private final String f_imageName;

  private ElementCategory(Element parent, String label, int imageFlags, String imageName) {
    super(parent);
    f_label = label;
    f_imageFlags = imageFlags;
    f_imageName = imageName;
  }

  @Override
  @NonNull
  Element[] constructChildren() {
    return f_children;
  }

  @Override
  String getLabel() {
    return f_label;
  }

  @Override
  int getImageFlags() {
    return f_imageFlags;
  }

  @Override
  @Nullable
  String getImageName() {
    return f_imageName;
  }

  @Override
  public int compareTo(Element o) {
    // TODO Auto-generated method stub
    return 0;
  }
}
