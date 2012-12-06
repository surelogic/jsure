package com.surelogic.jsure.client.eclipse.model.selection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.graphics.Image;

import com.surelogic.Nullable;
import com.surelogic.common.CommonImages;
import com.surelogic.common.ref.DeclUtil;
import com.surelogic.common.ref.IDecl;
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
    Image image = f_valueToImageName.get(value);
    if (image == null)
      return SLImages.getImage(CommonImages.IMG_CLASS);
    else
      return image;
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
            final IDecl enclosingType = DeclUtil.getTypeNotInControlFlow(jr.getDeclaration());
            if (enclosingType != null) {
              final Image valueImage = SLImages.getImageFor(enclosingType);
              f_valueToImageName.put(value, valueImage);
            }
          }
        } else {
          f_counts.put(value, count + 1);
        }
        runningTotal++;
      }
    }
    f_countTotal = runningTotal;
  }

  private final Map<String, Image> f_valueToImageName = new HashMap<String, Image>();

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
