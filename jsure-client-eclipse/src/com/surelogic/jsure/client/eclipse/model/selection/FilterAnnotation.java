package com.surelogic.jsure.client.eclipse.model.selection;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.graphics.Image;

import com.surelogic.Nullable;
import com.surelogic.common.CommonImages;
import com.surelogic.common.ui.SLImages;
import com.surelogic.dropsea.IPromiseDrop;
import com.surelogic.dropsea.IProofDrop;

public final class FilterAnnotation extends Filter implements IOnlyPromisesPorus {

  public static final ISelectionFilterFactory FACTORY = new AbstractFilterFactory() {
    @Override
    public Filter construct(Selection selection, Filter previous) {
      return new FilterAnnotation(selection, previous, getFilterLabel());
    }

    @Override
    public String getFilterLabel() {
      return "Annotation";
    }

    @Override
    public Image getFilterImage() {
      return SLImages.getImage(CommonImages.IMG_ANNOTATION);
    }
  };

  private FilterAnnotation(Selection selection, Filter previous, String filterLabel) {
    super(selection, previous, filterLabel);
  }

  @Override
  public ISelectionFilterFactory getFactory() {
    return FACTORY;
  }

  @Override
  public Image getImageFor(String value) {
    return SLImages.getImage(CommonImages.IMG_ANNOTATION);
  }

  @Override
  @Nullable
  public String getFilterValueFromDropOrNull(IProofDrop drop) {

    if (!(drop instanceof IPromiseDrop))
      return null;

    final String result = drop.getIRDropSeaClass().getSimpleName();

    // Special cases
    final String subst = substMap.get(result);
    if (subst != null) {
    	return subst;
    }
    // General case XResultDrop where we return X
    if (!result.endsWith(suffix))
      return null;

    return result.substring(0, result.length() - suffix.length());
  }
  
  private static final String suffix = "PromiseDrop";
  
  private static final Map<String,String> substMap = new HashMap<String, String>();
  static {
	  substMap.put("LockModel", "RegionLock");
	  substMap.put("RegionModel", "Region");
	  substMap.put("VouchFieldIsPromiseDrop", "Vouch");
	  substMap.put("ExplicitBorrowedInRegionPromiseDrop", "BorrowedInRegion");
	  substMap.put("ExplicitUniqueInRegionPromiseDrop", "UniqueInRegion");
	  substMap.put("SimpleBorrowedInRegionPromiseDrop", "BorrowedInRegion");
	  substMap.put("SimpleUniqueInRegionPromiseDrop", "UniqueInRegion");
  }
}
