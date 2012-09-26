package com.surelogic.jsure.client.eclipse.views.verification;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.common.CommonImages;
import com.surelogic.common.jsure.xml.CoE_Constants;
import com.surelogic.dropsea.IDrop;
import com.surelogic.dropsea.IPromiseDrop;
import com.surelogic.dropsea.IProofDrop;
import com.surelogic.dropsea.UiPlaceInASubFolder;
import com.surelogic.dropsea.ir.Category;

final class ElementCategory extends Element {

  public static final String SPECIAL_HINT_FOLDER_NAME = "Suggestions and warnings";

  static final class Categorizer {

    private final Element f_parent;
    private final List<IDrop> f_uncategorized = new ArrayList<IDrop>();
    final Map<Category, ElementCategory.Builder> f_categoryToBuilder = new HashMap<Category, ElementCategory.Builder>();

    Categorizer(Element parent) {
      f_parent = parent;
    }

    void add(IDrop drop) {
      if (drop == null)
        return;

      final Category category = drop.getCategory();
      if (category == null || isPromiseDropNotAtRoot(drop)) {
        f_uncategorized.add(drop);
      } else {
        ElementCategory.Builder builder = f_categoryToBuilder.get(category);
        if (builder == null) {
          builder = new ElementCategory.Builder();
          f_categoryToBuilder.put(category, builder);
          builder.setLabel(category.getMessage());
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
        return !drop.instanceOfIRDropSea(UiPlaceInASubFolder.class);
      } else
        return false;
    }

    boolean isEmpty() {
      return f_uncategorized.isEmpty() && f_categoryToBuilder.isEmpty();
    }

    Collection<ElementCategory.Builder> getBuilders() {
      // does not use f_parent
      return f_categoryToBuilder.values();
    }

    @NonNull
    List<Element> getUncategorizedElements() {
      final List<Element> result = new ArrayList<Element>();
      for (IDrop drop : f_uncategorized) {
        result.addAll(ElementDrop.factory(f_parent, drop));
      }
      return result;
    }

    @NonNull
    List<ElementCategory> getCategorizedElements() {
      final List<ElementCategory> result = new ArrayList<ElementCategory>();
      for (ElementCategory.Builder builder : getBuilders()) {
        builder.setParent(f_parent);
        result.add(builder.build());
      }
      return result;
    }

    @NonNull
    List<Element> getAllElements() {
      final List<Element> result = new ArrayList<Element>(getUncategorizedElements());
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
    private final List<IProofDrop> f_proofDrops = new ArrayList<IProofDrop>();
    boolean f_provedConsistent = true;
    boolean f_proofUsesRedDot = false;
    private final List<IDrop> f_otherDrops = new ArrayList<IDrop>();
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
      int flags = 0;
      if (!f_proofDrops.isEmpty()) {
        flags |= f_provedConsistent ? CoE_Constants.CONSISTENT : CoE_Constants.INCONSISTENT;
        if (f_proofUsesRedDot)
          flags |= CoE_Constants.REDDOT;
      }
      ElementCategory result = new ElementCategory(f_parent, f_label, flags, f_imageName);
      List<Element> children = new ArrayList<Element>();
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
}
