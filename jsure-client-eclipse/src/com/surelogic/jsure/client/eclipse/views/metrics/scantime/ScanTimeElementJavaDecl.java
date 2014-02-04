package com.surelogic.jsure.client.eclipse.views.metrics.scantime;

import org.eclipse.swt.graphics.Image;

import com.surelogic.NonNull;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.ref.DeclUtil;
import com.surelogic.common.ref.IDecl;
import com.surelogic.common.ui.SLImages;

public class ScanTimeElementJavaDecl extends ScanTimeElement {

  @NonNull
  final IDecl f_javaDecl;

  protected ScanTimeElementJavaDecl(ScanTimeElement parent, IDecl javaDecl) {
    super(parent, DeclUtil.getEclipseJavaOutlineLikeLabel(javaDecl));
    if (javaDecl == null)
      throw new IllegalArgumentException(I18N.err(44, "javaDecl"));
    else
      f_javaDecl = javaDecl;
  }

  @NonNull
  public IDecl getDeclaration() {
    return f_javaDecl;
  }

  @Override
  public Image getImage() {
    return SLImages.getImageFor(f_javaDecl);
  }

  @Override
  public boolean includeBasedOnAnalysisToShow(ScanTimeOptions options) {
    /*
     * This uses the analysis element answer as the answer for this element.
     * 
     * There should always be an analysis element up the chain from this
     * element.
     */
    ScanTimeElement analysis = this;
    do {
      analysis = analysis.getParent();
    } while (!(analysis instanceof ScanTimeElementAnalysis));
    return analysis.includeBasedOnAnalysisToShow(options);
  }
}
