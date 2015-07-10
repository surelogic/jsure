package com.surelogic.jsure.client.eclipse.views.status;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.graphics.Image;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.common.CommonImages;
import com.surelogic.common.i18n.I18N;
import com.surelogic.dropsea.IDrop;
import com.surelogic.dropsea.IPromiseDrop;
import com.surelogic.dropsea.IProofDrop;
import com.surelogic.jsure.client.eclipse.views.JSureDecoratedImageUtility;
import com.surelogic.jsure.client.eclipse.views.JSureDecoratedImageUtility.Flag;

final class ElementCategory extends Element {

  public static final String SPECIAL_HINT_FOLDER_NAME = "Suggestions and warnings";

  static final class Categorizer {

    private final Element f_parent;
    private final List<IDrop> f_uncategorized = new ArrayList<>();
    final Map<String, ElementCategory.Builder> f_categorizingStringToBuilder = new HashMap<>();

    Categorizer(Element parent) {
      f_parent = parent;
    }

    void add(IDrop drop) {
      if (drop == null)
        return;

      final String categorizingString = drop.getCategorizingMessage();
      if (categorizingString == null || isPromiseDropNotAtRoot(drop)) {
        f_uncategorized.add(drop);
      } else {
        ElementCategory.Builder builder = f_categorizingStringToBuilder.get(categorizingString);
        if (builder == null) {
          builder = new ElementCategory.Builder();
          f_categorizingStringToBuilder.put(categorizingString, builder);
          builder.setLabel(categorizingString);
        }
        builder.addDrop(drop);
      }
    }

    void addAll(Collection<? extends IDrop> drops) {
      if (drops == null)
        return;
      for (IDrop drop : drops)
        add(drop);
    }

    private boolean isPromiseDropNotAtRoot(IDrop drop) {
      /*
       * Special logic to identify promises not at the tree root except if the
       * drop type implements UiPlaceInASubFolder.
       */
      if (drop instanceof IPromiseDrop && f_parent != null) {
    	IPromiseDrop pd = (IPromiseDrop) drop;
    	return !pd.placeInASubFolder();
      } else
        return false;
    }

    boolean isEmpty() {
      return f_uncategorized.isEmpty() && f_categorizingStringToBuilder.isEmpty();
    }

    Collection<ElementCategory.Builder> getBuilders() {
      // does not use f_parent
      return f_categorizingStringToBuilder.values();
    }

    @NonNull
    List<Element> getUncategorizedElements() {
      final List<Element> result = new ArrayList<>();
      for (IDrop drop : f_uncategorized) {
        result.addAll(ElementDrop.factory(f_parent, drop));
      }
      return result;
    }

    @NonNull
    List<ElementCategory> getCategorizedElements() {
      final List<ElementCategory> result = new ArrayList<>();
      for (ElementCategory.Builder builder : getBuilders()) {
        builder.setParent(f_parent);
        result.add(builder.build());
      }
      return result;
    }

    @NonNull
    List<Element> getAllElements() {
      final List<Element> result = new ArrayList<>(getUncategorizedElements());
      result.addAll(getCategorizedElements());
      return result;
    }

    @NonNull
    Element[] getAllElementsAsArray() {
      final List<Element> result = getAllElements();
      return result.toArray(new Element[result.size()]);
    }
  }

  static final class Builder {

    private Element f_parent;
    private final List<IProofDrop> f_proofDrops = new ArrayList<>();
    boolean f_provedConsistent = true;
    boolean f_proofUsesRedDot = false;
    private final List<IDrop> f_otherDrops = new ArrayList<>();
    private final List<ElementCategory.Builder> f_categories = new ArrayList<>();
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

    void addDrop(IDrop drop) {
      if (drop == null)
        return;

      if (drop instanceof IProofDrop) {
        addProofDrop((IProofDrop) drop);
        return;
      }

      f_otherDrops.add(drop);
    }

    void addProofDrop(IProofDrop proofDrop) {
      if (proofDrop == null)
        return;
      if (proofDrop.proofUsesRedDot())
        f_proofUsesRedDot = true;
      f_provedConsistent &= proofDrop.provedConsistent();
      f_proofDrops.add(proofDrop);
    }

    void addCategory(ElementCategory.Builder builder) {
      if (builder == null)
        return;
      f_categories.add(builder);
    }

    void addCategories(Collection<ElementCategory.Builder> builders) {
      if (builders == null)
        return;
      for (ElementCategory.Builder builder : builders)
        addCategory(builder);
    }

    void setLabel(String value) {
      f_label = value;
    }

    void setImageName(String value) {
      f_imageName = value;
    }

    boolean isEmpty() {
      return f_proofDrops.isEmpty() && f_otherDrops.isEmpty() && f_categories.isEmpty();
    }

    ElementCategory build() {
      if (f_label == null)
        f_label = "NO LABEL";
      if (f_imageName == null) {
        if (f_proofDrops.isEmpty())
          f_imageName = CommonImages.IMG_INFO;
        else
          f_imageName = CommonImages.IMG_FOLDER;
      }
      if (isEmpty())
        throw new IllegalStateException("A category element must contain at least one element");

      /*
       * Determine necessary image flags
       */
      EnumSet<Flag> flags = EnumSet.noneOf(Flag.class);
      if (!f_proofDrops.isEmpty()) {
        flags.add(f_provedConsistent ? Flag.CONSISTENT : Flag.INCONSISTENT);
        if (f_proofUsesRedDot)
          flags.add(Flag.REDDOT);
      }
      ElementCategory result = new ElementCategory(f_parent, f_label, flags, f_imageName);
      List<Element> children = new ArrayList<>();
      for (IProofDrop pd : f_proofDrops) {
        children.addAll(ElementDrop.factory(result, pd));
      }
      for (IDrop hd : f_otherDrops) {
        children.addAll(ElementDrop.factory(result, hd));
      }
      for (ElementCategory.Builder builder : f_categories) {
        builder.setParent(result);
        children.add(builder.build());
      }
      result.f_children = children.toArray(new Element[children.size()]);
      return result;
    }
  }

  Element[] f_children;
  private final String f_label;
  private final EnumSet<Flag> f_imageFlags;
  private final String f_imageName;

  ElementCategory(Element parent, String label, EnumSet<Flag> imageFlags, String imageName) {
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
    return I18N.toStringForUIFolderLabel(f_label, getChildren().length);
  }

  @Override
  String getLabelToPersistViewerState() {
    return f_label;
  }

  @Override
  @Nullable
  Image getElementImage() {
    return JSureDecoratedImageUtility.getImage(f_imageName, f_imageFlags);
  }
}
