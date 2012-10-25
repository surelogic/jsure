package com.surelogic.jsure.client.eclipse.model.selection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.graphics.Image;

import com.surelogic.Nullable;
import com.surelogic.common.CommonImages;
import com.surelogic.common.ref.DeclUtil;
import com.surelogic.common.ref.IJavaRef;
import com.surelogic.common.ui.SLImages;
import com.surelogic.dropsea.IProofDrop;

public final class FilterJavaType extends Filter {

  public static final ISelectionFilterFactory FACTORY = new AbstractFilterFactory() {
    public Filter construct(Selection selection, Filter previous) {
      return new FilterJavaType(selection, previous, getFilterLabel());
    }

    public String getFilterLabel() {
      return "Java Type";
    }

    @Override
    public Image getFilterImage() {
      return SLImages.getImage(CommonImages.IMG_CLASS);
    }
  };

  private FilterJavaType(Selection selection, Filter previous, String filterLabel) {
    super(selection, previous, filterLabel);
  }

  @Override
  public ISelectionFilterFactory getFactory() {
    return FACTORY;
  }

  @Override
  public Image getImageFor(String value) {
    String imageName = f_valueToImageName.get(value);
    if (imageName == null)
      imageName = CommonImages.IMG_CLASS;

    return SLImages.getImage(imageName);
  }

  @Override
  protected void refreshCounts(List<IProofDrop> incomingResults) {
    f_counts.clear();
    f_valueToImageName.clear();
    int runningTotal = 0;
    for (IProofDrop d : incomingResults) {
      final String value = getFilterValueFromDropOrNull(d);
      if (value != null) {
        Integer count = f_counts.get(value);
        if (count == null) {
          f_counts.put(value, 1);
          final IJavaRef jr = d.getJavaRef();
          if (jr != null) {
            final String imageName;
            switch (DeclUtil.getTypeKind(jr.getDeclaration())) {
            case ANNOTATION:
              imageName = CommonImages.IMG_ANNOTATION;
              break;
            case ENUM:
              imageName = CommonImages.IMG_ENUM;
              break;
            case CLASS:
              imageName = CommonImages.IMG_CLASS;
              break;
            case INTERFACE:
              imageName = CommonImages.IMG_INTERFACE;
              break;
            default:
              imageName = CommonImages.IMG_CLASS;
            }
            f_valueToImageName.put(value, imageName);
          }
        } else {
          f_counts.put(value, count + 1);
        }
        runningTotal++;
      }
    }
    f_countTotal = runningTotal;
  }

  @Override
  protected void refreshPorousDrops(List<IProofDrop> incomingResults) {
    f_porousDrops.clear();
    for (IProofDrop d : incomingResults) {
      final String value = getFilterValueFromDropOrNull(d);
      if (value != null) {
        if (f_porousValues.contains(value)) {
          f_porousDrops.add(d);
        }
      }
    }
  }

  private final Map<String, String> f_valueToImageName = new HashMap<String, String>();

  @Override
  @Nullable
  public String getFilterValueFromDropOrNull(IProofDrop drop) {
    final IJavaRef jr = drop.getJavaRef();
    if (jr != null) {
      final String value = jr.getTypeNameOrNull();
      return value;
    }
    return null;
  }
}
