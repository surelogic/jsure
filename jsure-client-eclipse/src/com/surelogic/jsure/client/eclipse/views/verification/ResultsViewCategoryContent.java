package com.surelogic.jsure.client.eclipse.views.verification;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.surelogic.common.CommonImages;
import com.surelogic.common.jsure.xml.CoE_Constants;
import com.surelogic.dropsea.IDrop;
import com.surelogic.dropsea.IProofDrop;

public final class ResultsViewCategoryContent {

  public static final class Builder {
    private final List<IDrop> b_contents = new ArrayList<IDrop>();
    private String b_label;
    private String b_imageName;
    private int b_imageFlags;

    public void add(IDrop drop) {
      if (drop == null)
        return;
      b_contents.add(drop);
    }

    public void addAll(Collection<IDrop> drops) {
      if (drops == null)
        return;
      for (IDrop drop : drops)
        add(drop);
    }

    public void setLabel(String value) {
      b_label = value;
    }

    public void setImageName(String value) {
      b_imageName = value;
    }

    public ResultsViewCategoryContent build() {
      if (b_label == null)
        b_label = "NO LABEL";
      if (b_imageName == null)
        b_imageName = CommonImages.IMG_FOLDER;

      /*
       * Determine necessary image flags
       */
      boolean anyProofDrops = false;
      boolean provedConsistent = true;
      boolean proofUsesRedDot = false;
      boolean anyInfoHints = false;
      boolean anyWarningHints = false;
      for (IDrop drop : b_contents) {
        if (drop instanceof IProofDrop) {
          IProofDrop proofDrop = (IProofDrop) drop;
          anyProofDrops = true;
          if (proofDrop.proofUsesRedDot())
            proofUsesRedDot = true;
          provedConsistent &= proofDrop.provedConsistent();
        }
      }
      if (anyProofDrops) {
        b_imageFlags |= provedConsistent ? CoE_Constants.CONSISTENT : CoE_Constants.INCONSISTENT;
        if (proofUsesRedDot)
          b_imageFlags |= CoE_Constants.REDDOT;
      }
      if (anyInfoHints && !anyWarningHints) {
        b_imageFlags |= CoE_Constants.HINT_INFO;
      }
      if (anyWarningHints) {
        b_imageFlags |= CoE_Constants.HINT_WARNING;
      }
      return new ResultsViewCategoryContent(b_contents.toArray(new IDrop[b_contents.size()]), b_label, b_imageName, b_imageFlags);
    }
  }

  private ResultsViewCategoryContent(IDrop[] contents, String label, String imageName, int imageFlags) {
    f_contents = contents;
    f_imageName = imageName;
    f_imageFlags = imageFlags;
    f_label = label;
  }

  private final IDrop[] f_contents;

  public IDrop[] getContents() {
    return f_contents;
  }

  private final String f_label;

  public String getLabel() {
    return f_label;
  }

  private final String f_imageName;

  public String getImageName() {
    return f_imageName;
  }

  private final int f_imageFlags;

  public int getImageFlags() {
    return f_imageFlags;
  }
}
